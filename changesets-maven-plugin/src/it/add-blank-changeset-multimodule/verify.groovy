// Verify that exactly one markdown file was added to the .changeset folder
def rootcount = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .size()

assert rootcount == 1

// Verify that no files were added to the .changeset folders of the submodules
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


// Verify that the created changeset file has the expected content
def firstChangeset = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .first()
assert firstChangeset.text.equals("---\n" +
        "\"se.fortnox.maven.it:add-blank-changeset-multimodule\": patch\n" +
        "---\n" +
        "\n" +
        "thecontent")