package au.edu.rmit.tzar.parser.beans;

import au.edu.rmit.tzar.Utils;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Bean to represent the library configuration in the project config.
 */
public class LibraryBean {
  private String name;
  private String repo_type;
  private String url;
  private String revision;

  public static List<LibraryBean> fromLibraries(Map<String, CodeSourceImpl> libraries) {
    List<LibraryBean> beans = Lists.newArrayList();
    for (Map.Entry<String, CodeSourceImpl> library : libraries.entrySet()) {
      beans.add(LibraryBean.fromLibrary(library));
    }
    return beans;
  }

  public static LibraryBean fromLibrary(Map.Entry<String, CodeSourceImpl> library) {
    LibraryBean bean = new LibraryBean();
    bean.name = library.getKey();
    CodeSourceImpl source = library.getValue();
    bean.repo_type = source.getRepositoryType().name().toLowerCase();
    bean.revision = source.getRevision();
    bean.url = source.getSourceUri().toString();
    return bean;
  }

  public static Map<String, CodeSourceImpl> toLibraries(List<LibraryBean> libraryBeans) {
    Map<String, CodeSourceImpl> libraries = Maps.newHashMap();
    for (LibraryBean bean : libraryBeans) {
      libraries.put(bean.name, bean.toCodeSource());
    }
    return libraries;
  }

  private CodeSourceImpl toCodeSource() {
    return new CodeSourceImpl(Utils.makeAbsoluteUri(url),
        CodeSourceImpl.RepositoryTypeImpl.valueOf(repo_type.toUpperCase()), revision);
  }
}
