import groovy.xml.XmlSlurper

def project = new XmlSlurper().parse(new File(basedir, 'pom.xml'))
assert project.version == '2.0.0'

def submodule1 = new XmlSlurper().parse(new File(basedir, 'module1/pom.xml'))
// Check that the parent reference is updated to the new version
assert submodule1.parent.version == project.version
// TODO Check that the submodule version is synced with parent too, if set?

def submodule2 = new XmlSlurper().parse(new File(basedir, 'module2/pom.xml'))
// Check that the parent reference is updated to the new version
assert submodule2.parent.version == project.version

String changelog = new File(basedir, 'CHANGELOG.md').text;

assert changelog.contains("### Minor Changes")

assert changelog.equals("# multi-module\n" +
        "\n" +
        "## 2.0.0\n" +
        "\n" +
        "### Major Changes\n" +
        "\n" +
        "- A great change\n" +
        "\n" +
        "### Minor Changes\n" +
        "\n" +
        "- A medium change\n" +
        "\n" +
        "### Patch Changes\n" +
        "\n" +
        "- A tiny change\n")