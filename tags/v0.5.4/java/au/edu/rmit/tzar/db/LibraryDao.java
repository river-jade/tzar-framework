package au.edu.rmit.tzar.db;

import au.edu.rmit.tzar.api.CodeSource;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.Map;

/**
 * Data access object for libraries. Provides methods for loading libraries from the database.
 */
public class LibraryDao {

  /**
   * Associate the given libraries with the run specified by runId. If the libraries do not exist,
   * they will be created.
   * @param libraries libraries to associate
   * @param runId run to associate with
   * @param connection to connect to the db
   * @throws SQLException
   * @throws TzarException
   */
  void associateLibraries(Map<String, ? extends CodeSource> libraries, int runId, Connection connection)
      throws SQLException, TzarException {
    for (Map.Entry<String, ? extends CodeSource> entry : libraries.entrySet()) {
      String name = entry.getKey();
      CodeSource codeSource = entry.getValue();
      Optional<Library> library = findLibrary(connection, codeSource.getRepositoryType(), codeSource.getSourceUri(),
          name, codeSource.getRevision(), codeSource.isForceDownload());

      final int libraryId;
      if (!library.isPresent()) {
        libraryId = insertLibrary(name, codeSource.getRepositoryType(), codeSource.getSourceUri(),
            codeSource.getRevision(), codeSource.isForceDownload(), connection);
      } else {
        libraryId = library.get().id;
      }
      PreparedStatement statement = connection.prepareStatement("INSERT INTO run_libraries (run_id, " +
          "library_id) VALUES (?, ?)");
      statement.setInt(1, runId);
      statement.setInt(2, libraryId);
      statement.executeUpdate();
    }
  }

  /**
   * Insert the given library into the libraries table.
   *
   * @param name name of the library
   * @param repositoryType repo type for the library
   * @param sourceUri uri for the library
   * @param revision revision for the library
   * @param forceDownload whether to re-download the library each time it's needed
   * @param connection to connect to the db
   * @return the id of the new library
   * @throws SQLException
   * @throws TzarException
   */
  // TODO(river): use Library class when available
  int insertLibrary(String name, CodeSource.RepositoryType repositoryType, URI sourceUri, String revision,
      boolean forceDownload, Connection connection) throws SQLException, TzarException {

    PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO libraries (name, repo_type, uri, revision, force_download) VALUES (?, ?, ?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS);
    statement.setString(1, name);
    statement.setString(2, repositoryType.toString().toLowerCase());
    statement.setString(3, sourceUri.toString());
    statement.setString(4, revision);
    statement.setBoolean(5, forceDownload);
    statement.execute();
    ResultSet rs = statement.getGeneratedKeys();
    if (!rs.next()) {
      throw new TzarException("getGeneratedKeys returned an empty result set.");
    }
    return rs.getInt(1); // return the id of the newly created record
  }

  /**
   * Find a library matching the given parameters.
   *
   * @param connection db connection
   * @param repoType repo type of the library to find
   * @param uri uri of the library to find
   * @param name name of the library to find
   * @param revision revision of the library to find
   * @param forceDownload libraries with different values for forceDownload are treated as different libraries
   * @return an optional library. will be empty if none found
   * @throws SQLException
   */
  Optional<Library> findLibrary(Connection connection, CodeSource.RepositoryType repoType, URI uri, String name,
      String revision, boolean forceDownload) throws SQLException {
    PreparedStatement statement = connection.prepareStatement("SELECT library_id, repo_type, uri, name, revision, " +
        "force_download FROM libraries WHERE repo_type=? AND uri=? AND name=? AND revision=? AND force_download=?");
    statement.setString(1, repoType.toString().toLowerCase());
    statement.setString(2, uri.toString());
    statement.setString(3, name);
    statement.setString(4, revision);
    statement.setBoolean(5, forceDownload);
    ResultSet rs = statement.executeQuery();
    if (!rs.next()) {
      return Optional.absent();
    }
     return Optional.of(libraryFromResultSet(rs));
  }

  /**
   * Retrieve all libraries for the given runId.
   * @param runId id of the run to lookup
   * @param connection database connection
   * @return libraries map
   * @throws TzarException
   * @throws SQLException
   */
  ImmutableMap<String, CodeSource> getLibraries(int runId, Connection connection) throws TzarException, SQLException {
    PreparedStatement statement = connection.prepareStatement("SELECT l.library_id, repo_type, uri, name, " +
        "revision, force_download FROM libraries l INNER JOIN run_libraries rl ON l.library_id = rl.library_id " +
        "WHERE run_id = ?");

    statement.setInt(1, runId);
    ResultSet resultSet = statement.executeQuery();
    ImmutableMap.Builder<String, CodeSource> builder = ImmutableMap.builder();
    while (resultSet.next()) {
      try {
        Library library = libraryFromResultSet(resultSet);
        CodeSource libCodeSource = new CodeSourceImpl(new URI(library.uri),
            CodeSourceImpl.RepositoryTypeImpl.valueOf(library.repoType.toUpperCase()), library.revision,
            library.forceDownload);
        builder.put(library.name, libCodeSource);
      } catch (URISyntaxException e) {
        throw new TzarException(String.format("Invalid URI in database record. Run Id: %s", runId), e);
      }
    }
    return builder.build();
  }

  private Library libraryFromResultSet(ResultSet rs) throws SQLException {
    return new Library(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
        rs.getBoolean(6));
  }

  // TODO(river): make this part of the API. ie add a Libraries class to replace the ugly
  // Map<String, CodeSource> everywhere.
  private static class Library {
    final int id;
    final String repoType;
    final String uri;
    final String name;
    final String revision;
    final boolean forceDownload;

    public Library(int id, String repoType, String uri, String name, String revision, boolean forceDownload) {
      this.id = id;
      this.repoType = repoType;
      this.uri = uri;
      this.name = name;
      this.revision = revision;
      this.forceDownload = forceDownload;
    }
  }
}
