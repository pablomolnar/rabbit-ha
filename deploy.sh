version=$1
grails package-plugin 
mvn deploy:deploy-file -Dfile=rabbit-ha-${version}.zip -DrepositoryId=MLGrailsPlugins -Durl=http://git.ml.com:8081/nexus/content/repositories/MLGrailsPlugins -DgroupId=org.grails.plugins -DartifactId=rabbit-ha -Dversion=${version} -Dpackaging=zip 
