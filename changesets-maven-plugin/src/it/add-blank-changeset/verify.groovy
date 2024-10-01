def count = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .size()

assert count == 1