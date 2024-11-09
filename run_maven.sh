docker build -t my-maven-project .
docker run -it my-maven-project bash
docker run -it chattester-maven bash
#mvn clean install -X
#mvn io.github.ZJU-ACES-ISE:chattester-maven-plugin:1.4.1:method 
#mvn io.github.ZJU-ACES-ISE:chattester-maven-plugin:1.4.1:method -DselectMethod=XmlParser#parseMethodDescriptor