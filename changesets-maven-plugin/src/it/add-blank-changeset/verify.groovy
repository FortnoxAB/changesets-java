// Verify that exactly one markdown file was added to the .changeset folder
def count = new File(basedir, ".changeset")
        .listFiles()
        .findAll { it.name ==~ /.*\.md/ }
        .size()

assert count == 1