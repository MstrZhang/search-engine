package UTSCSearchEngine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.EmptyFileException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;

/**
 * Class for indexing files uploaded to the system
 */
public class Indexing {

  private StandardAnalyzer analyzer = null;
  private Directory index = null;
  private Path docDir = null;

  /**
   * Initialize and perform indexing with a given path, analyzer, to index (RAMDirectory)
   */
  public void doIndexing() {
    this.analyzer = new StandardAnalyzer();
    this.index = new RAMDirectory();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    try {
      config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
      IndexWriter w = new IndexWriter(index, config);
      indexDocuments(w);
      w.close();
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  /**
   * Overloaded indexing method for testing
   * @param url test database URL
   */
  public void doIndexing(String url) {
    this.analyzer = new StandardAnalyzer();
    this.index = new RAMDirectory();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    try {
      config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
      IndexWriter w = new IndexWriter(index, config);
      indexDocuments(url, w);
      w.close();
    } catch (IOException e) {
      System.err.println(e);
    }
  }

  /**
   * Index all the documents for a given Path
   * @param w an IndexWriter to write indexes
   * @throws IOException if the database return is invalid
   */
  private void indexDocuments(final IndexWriter w) throws IOException {
    Database db = new Database();
    ResultSet rs = null;

    try {
      rs = db.getAllFiles();
      while (rs.next()) {
        addDoc(w,
            rs.getString("id"),
            rs.getString("file_name"),
            rs.getString("file_type"),
            rs.getString("user_name"),
            rs.getString("user_type"),
            rs.getString("user_id"),
            rs.getString("file_size"),
            rs.getString("uploaded_on"),
            rs.getBytes("file"));
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * Overloaded method for testing
   * @param url test database URL
   * @param w IndexWriter to write indexes
   * @throws IOException if the database return is invalid
   */
  private void indexDocuments(String url, final IndexWriter w) throws IOException {
    Database db = new Database(url);
    ResultSet rs = null;

    try {
      rs = db.getAllFiles();
      while (rs.next()) {
        addDoc(w,
            rs.getString("id"),
            rs.getString("file_name"),
            rs.getString("file_type"),
            rs.getString("user_name"),
            rs.getString("user_type"),
            rs.getString("user_id"),
            rs.getString("file_size"),
            rs.getString("uploaded_on"),
            rs.getBytes("file"));
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  /**
   * Add all the documents' attributes to the index
   * @param w IndexWriter to write indexes
   * @param fileName the name of the given file
   * @param fileType the type of the given file (file extension)
   * @param userName the name of the user uploading the file
   * @param userType the type of the user uploading the file
   * @throws IOException if the database return is invalid
   */
  private static void addDoc(IndexWriter w, String fileId,
      String fileName, String fileType, String userName, String userType, String userId,
      String fileSize, String uploadDate, byte[] fileContents) throws IOException {
    Document doc = new Document();
    InputStream in = new ByteArrayInputStream(fileContents);

    // add the values to the index
    doc.add(new StringField("fileId", fileId, Field.Store.YES));
    doc.add(new TextField("fileName", fileName, Field.Store.YES));
    doc.add(new TextField("fileType", fileType, Field.Store.YES));
    doc.add(new TextField("userName", userName.replaceAll("%20", " "), Field.Store.YES));
    doc.add(new StringField("userType", userType, Field.Store.YES));
    doc.add(new TextField("userId", userId, Field.Store.YES));
    doc.add(new TextField("fileSize", fileSize, Field.Store.YES));
    doc.add(new TextField("uploadDate", uploadDate, Field.Store.YES));

    // restrict content analysis
    if (fileType.contains("docx") || fileType.contains("html") || fileType.contains("txt") ||
        fileType.contains("pdf")) {
      // if the file is a docx
      if (fileType.contains("docx")) {
        List<String> docContents = parseDocContents(fileContents);
        String contentString = "";
        // add all contents to a single string, ensure the contents of the file is not empty
        if (docContents != null) {
          for (String content : docContents) {
            if (content != null) {
              // store all content to a single string
              contentString = contentString.concat(content + " ");
            }
          }
        }
        // add the doc contents
        doc.add(new TextField("contents", contentString, Field.Store.YES));

      } else if (fileType.contains("html")) { // if the file is an html
        org.jsoup.nodes.Document html = Jsoup.parse(in, "UTF-8", "/");
        String contentsString = html.body().text();
        // add the txt contents
        doc.add(new TextField("contents", contentsString, Field.Store.YES));

      } else if (fileType.contains("pdf")) { // if the file is a pdf
        PDDocument pdf = PDDocument.load(fileContents);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setLineSeparator("\n");
        String contentsString = stripper.getText(pdf).replace("\n",
            " ").replace("\r", "");
        pdf.close();
        doc.add(new TextField("contents", contentsString, Field.Store.YES));

      } else { // otherwise if its a generic text file
        Scanner contentsScanner = new Scanner(in);
        String contentsString = "";
        if (contentsScanner.hasNextLine()) {
          contentsString =
              contentsScanner.useDelimiter("\\A").next().replace("\n",
                  " ").replace("\r", "");
        }
        // add the txt contents
        doc.add(new TextField("contents", contentsString, Field.Store.YES));
        contentsScanner.close();
      }
    }

    w.addDocument(doc);
    w.commit();
  }

  /**
   * Parses the contents of a file with a ".doc" or ".docx" extension
   * @param file the DOC file to be parsed
   * @return a List of the contents in the DOC file
   */
  private static List<String> parseDocContents(byte[] file) {
    List<XWPFParagraph> paragraphs;
    List<String> fileData = new ArrayList<>();
    try {
      // get all paragraphs of the documents
      InputStream in = new ByteArrayInputStream(file);
      XWPFDocument document = new XWPFDocument(in);
      paragraphs = document.getParagraphs();
      document.close();
      // add all the text of the document to the List of strings
      for (XWPFParagraph paragraph : paragraphs) {
        fileData.add(paragraph.getText());
      }
    } catch (EmptyFileException e) {
      // if the file is empty, who cares
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return fileData;
  }

  /**
   * Returns the index
   * @return the index
   */
  public Directory getIndex() {
    return this.index;
  }

  /**
   * Returns the indexer analyzer
   * @return the indexer analyzer
   */
  public StandardAnalyzer getAnalyzer() {
    return this.analyzer;
  }

  /**
   * Returns the path of the RAMDirectory
   * @return the path of the RAMDirectory
   */
  public Path getDocDir() {
    return this.docDir;
  }

}
