def firstChangeset = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .first()

assert firstChangeset.text.equals("---\n" +
        "\"com.fortnox.maven.it:add-blank-changelog\": patch\n" +
        "---\n" +
        "\n" +
        "thecontent");