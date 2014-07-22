package au.edu.rmit.tzar.parser.beans;

import au.edu.rmit.tzar.api.Parameters;
import au.edu.rmit.tzar.api.Repetitions;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.parser.ProjectSpecImpl;
import au.edu.rmit.tzar.repository.CodeSourceFactory;
import au.edu.rmit.tzar.repository.CodeSourceImpl;
import au.edu.rmit.tzar.runners.mapreduce.MapReduce;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * Bean to represent the project configuration.
 */
public class ProjectSpecBean {
  private Map<String, Object> base_params;
  private List<ScenarioBean> scenarios;
  private String project_name;
  private String runner_class;
  private String runner_flags;
  private MapReduceBean mapreduce;
  private ConcatenateBean concatenate;
  private List<LibraryBean> libraries;
  private RepetitionsBean repetitions;

  public static ProjectSpecBean fromProjectSpec(ProjectSpecImpl spec) {
    ProjectSpecBean bean = new ProjectSpecBean();
    bean.base_params = spec.getBaseParams().asMap();
    bean.scenarios = ScenarioBean.fromScenarios(spec.getScenarios());
    bean.project_name = spec.getProjectName();
    bean.runner_class = spec.getRunnerClass();
    bean.runner_flags = spec.getRunnerFlags();
    bean.libraries = LibraryBean.fromLibraries(spec.getLibraries());
    bean.repetitions = RepetitionsBean.fromRepetitions(spec.getRepetitions());
    bean.mapreduce = MapReduceBean.fromMapReduce(spec.getMapReduce());
    return bean;
  }

  public ProjectSpecImpl toProjectSpec(CodeSourceFactory codeSourceFactory) throws TzarException {
    Repetitions reps = (repetitions == null ? Repetitions.EMPTY_REPETITIONS : repetitions.toRepetitions());
    Map<String, CodeSourceImpl> libs = (libraries == null ? ImmutableMap.<String, CodeSourceImpl>of() :
        LibraryBean.toLibraries(libraries, codeSourceFactory));

    MapReduce mapReduce = null;
    if (mapreduce != null) {
      mapReduce = mapreduce.toMapReduce();
    } else if (concatenate != null) {
      mapReduce = concatenate.toMapReduce();
    }

    List<String> errors = Lists.newArrayList();
    if (base_params == null) {
      errors.add("Base parameters must be set.");
    }
    if (project_name == null) {
      errors.add("Project name must be set.");
    }
    if (runner_class == null) {
      errors.add("Runner class must be set.");
    }
    if (!errors.isEmpty()) {
      throw new TzarException(String.format("Errors parsing project spec: [\n%s\n]", Joiner.on("\n").join(errors)));
    }

    return new ProjectSpecImpl(project_name, runner_class, Strings.nullToEmpty(runner_flags),
        Parameters.createParameters(base_params), ScenarioBean.toScenarios(scenarios), reps, libs, mapReduce);
  }
}
