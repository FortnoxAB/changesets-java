import groovy.xml.XmlSlurper

def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))

String version = new File(basedir, '.changeset/VERSION').text;
assert version == '2.5.0'
assert version == project.version.toString()
