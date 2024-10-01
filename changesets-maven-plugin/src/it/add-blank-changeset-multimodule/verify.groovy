def module1count = new File(basedir, "./module1/.changeset/")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .size()

assert module1count == 0

def module2count = new File(basedir, "./module2/.changeset/")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .size()

assert module2count == 0

def rootcount = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .size()

assert rootcount == 1

def firstChangeset = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .first()
assert firstChangeset.text.equals("---\n" +
        "\"se.fortnox.maven.it:add-blank-changeset-multimodule\": patch\n" +
        "---\n" +
        "\n" +
        "thecontent")