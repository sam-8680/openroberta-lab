package de.fhg.iais.roberta.syntax.codegen.raspberrypi;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.fhg.iais.roberta.components.ConfigurationAst;
import de.fhg.iais.roberta.util.test.UnitTestHelper;

@Ignore
public class PythonVisitorTest extends RaspberryPiAstTest {

    private static final String IMPORTS =
        "" //
            + "#!/usr/bin/python\n\n"
            + "from __future__ import absolute_import\n"
            + "from roberta import Hal\n"
            + "from roberta import BlocklyMethods\n"
            + "import math\n\n"
            + "class BreakOutOfALoop(Exception): pass\n"
            + "class ContinueLoop(Exception): pass\n\n";

    private static final String GLOBALS = "hal = Hal()\n";

    private static final String MAIN_METHOD =
        "" //
            + "def main():\n"
            + "    try:\n"
            + "        run()\n"
            + "    except Exception as e:\n"
            + "        print('Fehler im Vorwerk')\n"
            + "        print(e.__class__.__name__)\n"
            + "        print(e)\n"
            + "if __name__ == \"__main__\":\n"
            + "    main()";
    private static ConfigurationAst brickConfiguration;

    @BeforeClass
    public static void setupConfigurationForAllTests() {
        ConfigurationAst.Builder builder = new ConfigurationAst.Builder();
        brickConfiguration = builder.build();
    }

    @Test
    public void visitTouchSensor_GetValuesFromAllPortsAndSlots_ReturnsCorrectPythonProgram() throws Exception {
        String expectedResult =
            "" //
                + IMPORTS
                + GLOBALS
                + "\nitem = True\n"
                + "def run():\n"
                + "    global item\n"
                + "    item = hal.sample_touch_sensor('left', 'front')\n"
                + "    item = hal.sample_touch_sensor('left', 'side')\n"
                + "    item = hal.sample_touch_sensor('right', 'front')\n"
                + "    item = hal.sample_touch_sensor('right', 'side')\n"
                + "\n"
                + MAIN_METHOD;

        UnitTestHelper.checkGeneratedSourceEqualityWithProgramXmlAndSourceAsString(testFactory, expectedResult, "/sensors/touch.xml", false);

    }


}
