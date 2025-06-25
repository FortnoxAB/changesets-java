def changesetFiles = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
assert changesetFiles.size() == 3


assert new File(basedir, ".changeset/patch.md").text.equals("---\n" +
        "\"add-blank-changelog\": patch\n" +
        "---\n" +
        "\n" +
        "Patch change");

assert new File(basedir, ".changeset/minor.md").text.equals("---\n" +
        "\"add-blank-changelog\": minor\n" +
        "---\n" +
        "\n" +
        "Minor change");

assert new File(basedir, ".changeset/major.md").text.equals("---\n" +
        "\"add-blank-changelog\": major\n" +
        "---\n" +
        "\n" +
        "Major change");

// See invoker.properties for the dependency change details
/*
assert new File(basedir, ".changeset/dependency.md").text.equals("---\n" +
        "\"add-blank-changelog\": dependency\n" +
        "---\n" +
        "\n" +
        "Dependency change");
*/