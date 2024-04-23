#!/bin/bash

declare -a targets=("172.234.204.189:chicago" "172.234.92.219:osaka" "172.232.132.128:stockholm" "172.232.206.111:milan" "172.233.16.18:sao-paulo")
orchestrator="172.234.204.189:chicago"

echo "Building reader"
pushd reader
./gradlew build assemble
popd

echo "Building writer"
pushd writer
./gradlew build assemble
popd

echo "Building orchestrator"
pushd orchestrator
./gradlew build assemble
popd

for target in "${targets[@]}"
do
    url=$(echo $target | cut -d ":" -f 1)
    location=$(echo $target | cut -d ":" -f 2)
    echo "Deploying reader to $location"
    scp -i ./global_perf reader/build/libs/reader-0.0.1-SNAPSHOT.jar root@${url}:/root/

    echo "Deploying writer to $location"
    scp -i ./global_perf writer/build/libs/writer-0.0.1-SNAPSHOT.jar root@${url}:/root/

echo "Starting reader and writer in $location"
ssh -t -o IdentitiesOnly=yes -i ./global_perf root@$url << EOF
    killall -9 java
    cd /root
    nohup java -DdcLocation=$location -jar reader-0.0.1-SNAPSHOT.jar > reader.log 2>&1 &
    nohup java -DdcLocation=$location -jar writer-0.0.1-SNAPSHOT.jar > writer.log 2>&1 &

EOF

done

orchestrator_url=$(echo $orchestrator | cut -d ":" -f 1)
orchestrator_location=$(echo $orchestrator | cut -d ":" -f 2)


echo "Deploying orchestrator to $orchestrator_location"
scp -i ./global_perf orchestrator/build/libs/orchestrator-0.0.1-SNAPSHOT.jar root@${orchestrator_url}:/root/

echo "Starting orchestrator in $orchestrator_location"
ssh -t -o IdentitiesOnly=yes -i ./global_perf root@$orchestrator_url << EOF
    cd /root
    nohup java -DdcLocation=$orchestrator_location -jar orchestrator-0.0.1-SNAPSHOT.jar > orchestrator.log 2>&1 &

EOF