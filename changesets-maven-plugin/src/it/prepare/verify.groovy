import groovy.xml.XmlSlurper

def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assert project.version == '2.5.1-SNAPSHOT'

String version = new File(basedir, '.changeset/VERSION').text;
assert version == '2.5.0'

// TODO Check the CHANGELOG file
