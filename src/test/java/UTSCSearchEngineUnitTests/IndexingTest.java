package UTSCSearchEngineUnitTests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import UTSCSearchEngine.Indexing;
import static org.junit.Assert.*;

public class IndexingTest {

  // create temporary folder for testing
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testDoIndexingTXT() throws ParseException, NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException, IOException {
    // this folder gets cleaned up automatically by JUnit
    File file1 = folder.newFile("test file 1.txt");
    File file2 = folder.newFile("test file 2.txt");
    File file3 = folder.newFile("test file 3.txt");

    // set up expected lines
    String file1Line1 = "Line 1 foo";
    String file1Line2 = "Line 2 fighters";
    String file2Line1 = "Line 1 foo";
    String file2Line2 = "Line 2 bar";

    List<String> expectedContents = new ArrayList<String>();
    expectedContents.add("Line 1 foo\nLine 2 fighters\n");
    expectedContents.add("Line 1 foo\nLine 2 fighters\n");
    expectedContents.add("Line 1 foo\nLine 2 bar\n");


    // set up contents for the test files
    List<String> file1Contents = new ArrayList<String>();
    List<String> file2Contents = new ArrayList<String>();

    file1Contents.add(file1Line1);
    file1Contents.add(file1Line2);
    file2Contents.add(file2Line1);
    file2Contents.add(file2Line2);

    writeToFile(file1, file1Contents);
    writeToFile(file2, file2Contents);
    writeToFile(file3, file1Contents);

    List<String> expectedFileNames =
        Arrays.asList("test file 1.txt", "test file 2.txt", "test file 3.txt");

    Indexing indexer = new Indexing();

    // indexer.setDocsPath(folder.getRoot().toString());
    Field field = indexer.getClass().getDeclaredField("docsPath");
    field.setAccessible(true);
    field.set(indexer, folder.getRoot().toString());
    indexer.doIndexing();

    // search for all test files
    Query q = new QueryParser("fileName", indexer.getAnalyzer()).parse("test");
    IndexReader reader = DirectoryReader.open(indexer.getIndex());
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs docs = searcher.search(q, 10);
    ScoreDoc[] hits = docs.scoreDocs;
    // System.out.println(reader.toString());

    // get all the file names and contents
    List<String> titleList = new ArrayList<String>();
    List<String> contentsList = new ArrayList<String>();
    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      Document d = searcher.doc(docId);
      titleList.add(d.get("fileName"));
      contentsList.add(d.get("contents"));
    }

    Collections.sort(titleList);
    assertEquals("all files must be accounted for and correct", expectedFileNames, titleList);
    assertEquals("all contents must be accounted for and correct", expectedContents, contentsList);

  }

  @Test
  public void testDoIndexingDoc() throws ParseException, NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException, IOException {
    // create word docs for testing

    // set up paragraphs
    String content1 = "The quick brown fox jumped over the lazy dog.";
    String content2 =
        "This section gives instructions on how to format and generate a Microsoft Word file.";
    String content3 =
        "Notice that we hard-code the contents of both the title and subtitle as these statements are too short to justify the use of a helper method.";

    // create the docx files
    CreateDOCX.create(folder.getRoot().toString(), "test file 1.docx", content1);
    CreateDOCX.create(folder.getRoot().toString(), "test file 2.docx", content2);
    CreateDOCX.create(folder.getRoot().toString(), "test file 3.docx", content3);
    
    List<String> expectedFileNames =
        Arrays.asList("test file 1.docx", "test file 2.docx", "test file 3.docx");

    Indexing indexer = new Indexing();

    // indexer.setDocsPath(folder.getRoot().toString());
    Field field = indexer.getClass().getDeclaredField("docsPath");
    field.setAccessible(true);
    field.set(indexer, folder.getRoot().toString());
    indexer.doIndexing();

    // search for all test files
    Query q = new QueryParser("fileName", indexer.getAnalyzer()).parse("test");
    IndexReader reader = DirectoryReader.open(indexer.getIndex());
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs docs = searcher.search(q, 10);
    ScoreDoc[] hits = docs.scoreDocs;
    // System.out.println(reader.toString());

    // get all the file names and contents
    List<String> titleList = new ArrayList<String>();
    List<String> contentsList = new ArrayList<String>();
    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      Document d = searcher.doc(docId);
      titleList.add(d.get("fileName"));
      contentsList.add(d.get("contents"));
    }

    Collections.sort(titleList);
    assertEquals("all files must be accounted for and correct", expectedFileNames, titleList);
    
    Boolean containsContent1 = false;
    Boolean containsContent2 = false;
    Boolean containsContent3 = false;
    for (String content : contentsList) {
      containsContent1 = containsContent1 || content.contains(content1);
      containsContent2 = containsContent2 || content.contains(content2);
      containsContent3 = containsContent3|| content.contains(content3);
    }
    assertTrue("Content 1 must be accounted for and correct", containsContent1);
    assertTrue("Content 2 must be accounted for and correct", containsContent2);
    assertTrue("Content 3 must be accounted for and correct", containsContent3);

  }


  @Test
  public void testSetDocsPath() throws NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    Indexing indexer = new Indexing();
    indexer.setDocsPath("examplepath");

    Field field = indexer.getClass().getDeclaredField("docsPath");
    field.setAccessible(true);
    assertEquals("Docs path didn't match", field.get(indexer), "examplepath");
  }

  @Test
  public void testGetIndex() throws NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    Indexing indexer = new Indexing();
    Directory testDir = new RAMDirectory();
    Field field = indexer.getClass().getDeclaredField("index");
    field.setAccessible(true);
    field.set(indexer, testDir);
    final Directory result = indexer.getIndex();

    assertEquals("Directory wasn't retrieved properly", testDir, result);

  }

  @Test
  public void testGetAnlyzer() throws NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    Indexing indexer = new Indexing();
    StandardAnalyzer testAnalyzer = new StandardAnalyzer();
    Field field = indexer.getClass().getDeclaredField("analyzer");
    field.setAccessible(true);
    field.set(indexer, testAnalyzer);
    final StandardAnalyzer result = indexer.getAnalyzer();

    assertEquals("Analyzer wasn't retrieved properly", testAnalyzer, result);
  }

  @Test
  public void testGetDocDir() throws NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    Indexing indexer = new Indexing();
    Path testPath = null;
    Field field = indexer.getClass().getDeclaredField("analyzer");
    field.setAccessible(true);
    field.set(indexer, testPath);
    final Path result = indexer.getDocDir();

    assertEquals("Path wasn't retrieved properly", testPath, result);
  }

  /**
   * Method that writes to given file for given contents
   * 
   * @param file
   * @param contents
   */
  private void writeToFile(File file, List<String> contents) {
    BufferedWriter br = null;
    try {
      br = new BufferedWriter(new FileWriter(file));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    PrintWriter out = new PrintWriter(br);

    // setup contents of temp file
    for (String line : contents) {
      out.println(line);
    }
    out.close();
  }

}
