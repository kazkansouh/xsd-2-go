
set -xe

xsd=$1
xjc -quiet -p generated "$xsd"
cd ./generated
javac *.java
cd ..
java -jar xsd2go.jar ./generated/ generated
rm -rf ./generated
