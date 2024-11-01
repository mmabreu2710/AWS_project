# criar config.sh primeiro, ver lab-aws
source config.sh
# fazer download da sdk, ver lab-aws
java -cp aws-java-sdk-1.12.729/lib/aws-java-sdk-1.12.729.jar:aws-java-sdk-1.12.729/third-party/lib/*:. db_handler/src/main/java/pt/ulisboa/tecnico/cnv/db_handler/RayTracer_DB.java "$@"
