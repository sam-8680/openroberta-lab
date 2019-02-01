#!/bin/bash

isServerNameValid $SERVER_NAME
SERVERDIR=$SERVER/$SERVER_NAME
isDirectoryValid $SERVERDIR

source $SERVERDIR/decl.sh
isDefined PORT
isDefined DATE_SETUP
isDefined BRANCH
isDefined GIT_REPO
case "$c" in
    /*) : ;;
    *)  GIT_REPO=$BASE/$GIT_REPO
esac

# ----- AQUIRE A FILE LOCK. Checkout commit, build binaries, export, build container -----
( flock -w 1200 9 || (echo "deployement of $SERVER_NAME was delayed for more than 20 minutes. Exit 12"; exit 12) 
    cd $GIT_REPO
    if [ "$COMMIT" == '' ]
    then
        echo "checking out branch $BRANCH and get the actual state"
        git pull
        git checkout $BRANCH
        git pull
        LAST_COMMIT=$(git rev-list HEAD...HEAD~1)
    else
        echo "checking out commit $COMMIT"
        git checkout $COMMIT
        LAST_COMMIT=$COMMIT
    fi
    cd OpenRobertaParent; mvn clean install -DskipTests; cd ..
    rm -rf $SERVERDIR/export
    ./ora.sh --export $SERVERDIR/export gzip
    cp $CONF/docker-for-lab/start.sh $SERVERDIR/export
    DOCKERRM=$(docker rmi rbudde/openroberta_lab_$SERVER_NAME:1 2>/dev/null)
    case "$DOCKERRM" in
        '') echo "found no docker image 'rbudde/openroberta_lab_$SERVER_NAME:1'. No image removed" ;;
        * ) echo "removed docker image 'rbudde/openroberta_lab_$SERVER_NAME:1'" ;;
    esac
    docker build -f $CONF/docker-for-lab/DockerfileLab -t rbudde/openroberta_lab_$SERVER_NAME:1 $SERVERDIR/export
    
    DATE_DEPLOY=$(date --rfc-3339=seconds)
    cat >$SERVERDIR/deploy.txt <<.EOF
HOSTNAME = $HOSTNAME
DATE_SETUP = $DATE_SETUP
DATE_DEPLOY = $DATE_DEPLOY
GIT_REPO = $GIT_REPO
BRANCH = $BRANCH
LAST_COMMIT = $LAST_COMMIT
PORT = $PORT
.EOF

) 9>$SERVER/lockfile