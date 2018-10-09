package de.fhg.iais.roberta.codegen;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.blockly.generated.BlockSet;
import de.fhg.iais.roberta.codegen.AbstractCompilerWorkflow;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.arduino.ArduinoConfiguration;
import de.fhg.iais.roberta.factory.IRobotFactory;
import de.fhg.iais.roberta.inter.mode.action.ILanguage;
import de.fhg.iais.roberta.transformer.BlocklyProgramAndConfigTransformer;
import de.fhg.iais.roberta.transformers.arduino.Jaxb2ArduinoConfigurationTransformer;
import de.fhg.iais.roberta.util.Key;
import de.fhg.iais.roberta.util.jaxb.JaxbHelper;
import de.fhg.iais.roberta.visitor.codegen.ArduinoCppVisitor;

public class ArduinoCompilerWorkflow extends AbstractCompilerWorkflow {

    private static final Logger LOG = LoggerFactory.getLogger(ArduinoCompilerWorkflow.class);

    public final String pathToCrosscompilerBaseDir;
    public final String robotCompilerResourcesDir;
    public final String robotCompilerDir;
    private final String robot;

    private String compiledHex = "error";

    public ArduinoCompilerWorkflow(String pathToCrosscompilerBaseDir, String robotCompilerResourcesDir, String robotCompilerDir, String robot) {
        this.pathToCrosscompilerBaseDir = pathToCrosscompilerBaseDir;
        this.robotCompilerResourcesDir = robotCompilerResourcesDir;
        this.robotCompilerDir = robotCompilerDir;
        this.robot = robot;
    }

    @Override
    public String generateSourceCode(String token, String programName, BlocklyProgramAndConfigTransformer data, ILanguage language) {
        if ( data.getErrorMessage() != null ) {
            return null;
        }
        try {
            ArduinoConfiguration configuration = ((ArduinoConfiguration) data.getBrickConfiguration());
            String sourceCode = ArduinoCppVisitor.generate(configuration, data.getProgramTransformer().getTree(), true);
            ArduinoCompilerWorkflow.LOG.info("generating arduino c++ code");
            return sourceCode;
        } catch ( Exception e ) {
            LOG.error("generating source code failed", e);
            return null;
        }
    }

    @Override
    public Key compileSourceCode(String token, String programName, String sourceCode, ILanguage language, Object flagProvider) {
        try {
            storeGeneratedProgram(token, programName, sourceCode, this.pathToCrosscompilerBaseDir, ".ino");
        } catch ( Exception e ) {
            ArduinoCompilerWorkflow.LOG.error("Storing the generated program into directory " + token + " failed", e);
            return Key.COMPILERWORKFLOW_ERROR_PROGRAM_STORE_FAILED;
        }

        Key messageKey = runBuild(token, programName, "generated.main");
        if ( messageKey == Key.COMPILERWORKFLOW_SUCCESS ) {
            ArduinoCompilerWorkflow.LOG.info("hex for program {} generated successfully", programName);
        } else {
            ArduinoCompilerWorkflow.LOG.info(messageKey.toString());
        }
        return sourceCode == null ? Key.COMPILERWORKFLOW_ERROR_PROGRAM_COMPILE_FAILED : messageKey;
    }

    @Override
    public Configuration generateConfiguration(IRobotFactory factory, String blocklyXml) throws Exception {
        BlockSet project = JaxbHelper.xml2BlockSet(blocklyXml);
        Jaxb2ArduinoConfigurationTransformer transformer = new Jaxb2ArduinoConfigurationTransformer(factory);
        return transformer.transform(project);
    }

    @Override
    public String getCompiledCode() {
        return this.compiledHex;
    }

    /**
     * 1. Make target folder (if not exists).<br>
     * 2. Clean target folder (everything inside).<br>
     * 3. Compile .java files to .class.<br>
     * 4. Make jar from class files and add META-INF entries.<br>
     *
     * @param token
     * @param mainFile
     * @param mainPackage
     */
    private Key runBuild(String token, String mainFile, String mainPackage) {
        final StringBuilder sb = new StringBuilder();
        String scriptName = "";
        String os = "";
        System.out.println(System.getProperty("os.arch"));
        if ( SystemUtils.IS_OS_LINUX ) {
            if ( System.getProperty("os.arch").contains("arm") ) {
                scriptName = this.robotCompilerResourcesDir + "/linux-arm/arduino-builder";
                os = "linux-arm";
            } else {
                scriptName = this.robotCompilerResourcesDir + "/linux/arduino-builder";
                os = "linux";
            }
        } else if ( SystemUtils.IS_OS_WINDOWS ) {
            scriptName = this.robotCompilerResourcesDir + "/windows/arduino-builder.exe";
            os = "windows";
        } else if ( SystemUtils.IS_OS_MAC ) {
            scriptName = this.robotCompilerResourcesDir + "/osx/arduino-builder";
            os = "osx";
        }
        Path path = Paths.get(this.pathToCrosscompilerBaseDir + token + "/" + mainFile);
        Path base = Paths.get("");

        try {
            String fqbnArg = "";
            if ( this.robot.equals("uno") ) {
                fqbnArg = "-fqbn=arduino:avr:uno";
            } else if ( this.robot.equals("mega") ) {
                fqbnArg = "-fqbn=arduino:avr:mega:cpu=atmega2560";
            } else if ( this.robot.equals("nano") ) {
                fqbnArg = "-fqbn=arduino:avr:nano:cpu=atmega328";
            }

            ProcessBuilder procBuilder =
                new ProcessBuilder(
                    new String[] {
                        scriptName,
                        "-hardware=" + this.robotCompilerResourcesDir + "/hardware",
                        "-tools=" + this.robotCompilerResourcesDir + "/" + os + "/tools-builder",
                        "-libraries=" + this.robotCompilerResourcesDir + "/libraries",
                        fqbnArg,
                        "-prefs=compiler.path=" + this.robotCompilerDir,
                        "-build-path=" + base.resolve(path).toAbsolutePath().normalize().toString() + "/target/",
                        base.resolve(path).toAbsolutePath().normalize().toString() + "/src/" + mainFile + ".ino"
                    });

            procBuilder.redirectInput(Redirect.INHERIT);
            procBuilder.redirectOutput(Redirect.INHERIT);
            procBuilder.redirectError(Redirect.INHERIT);
            Process p = procBuilder.start();
            int ecode = p.waitFor();
            System.err.println("Exit code " + ecode);

            if ( ecode != 0 ) {
                return Key.COMPILERWORKFLOW_ERROR_PROGRAM_COMPILE_FAILED;
            }
            this.compiledHex = FileUtils.readFileToString(new File(path + "/target/" + mainFile + ".ino.hex"), "UTF-8");
            Base64.Encoder urec = Base64.getEncoder();
            this.compiledHex = urec.encodeToString(this.compiledHex.getBytes());
            return Key.COMPILERWORKFLOW_SUCCESS;
        } catch ( Exception e ) {
            if ( sb.length() > 0 ) {
                ArduinoCompilerWorkflow.LOG.error("build exception. Messages from the build script are:\n" + sb.toString(), e);
            } else {
                ArduinoCompilerWorkflow.LOG.error("exception when preparing the build", e);
            }
            return Key.COMPILERWORKFLOW_ERROR_PROGRAM_COMPILE_FAILED;
        }
    }
}
