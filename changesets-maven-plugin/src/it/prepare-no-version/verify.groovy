import groovy.xml.XmlSlurper

def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assert project.version == '0.1.1-SNAPSHOT'


String version = new File(basedir, '.changeset/VERSION').text;
assert version == '0.1.0'

// TODO Check the CHANGELOG file