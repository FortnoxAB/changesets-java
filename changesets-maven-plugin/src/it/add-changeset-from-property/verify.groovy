// Verify that exactly one markdown file was added to the .changeset folder and has the expected content
def firstChangeset = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .first()

assert firstChangeset.text.equals("---\n" +
        "\"com.fortnox.maven.it:add-blank-changelog\": patch\n" +
        "---\n" +
        "\n" +
        "thecontent");