import groovy.xml.XmlSlurper

def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assert project.version == '2.5.0'

String version = new File(basedir, '.changeset/VERSION').text;
assert version == project.version.toString()

def submodule1 = new XmlSlurper().parse(new File(basedir, 'module1/pom.xml'))
// Check that the parent reference is updated to the new version
assert submodule1.parent.version == project.version
// TODO Check that the submodule version is synced with parent too, if set?

def submodule2 = new XmlSlurper().parse(new File(basedir, 'module2/pom.xml'))
// Check that the parent reference is updated to the new version
assert submodule2.parent.version == project.version

// TODO Assert CHANGELOG file