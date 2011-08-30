package au.edu.rmit.tzar.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class to hold repetitions definition. A set of repetitions is a set of parameter overrides,
 * which are used to override scenario parameters.
 */
public class Repetitions {
  @SerializedName("repetitions")
  private final List<Parameters> params;

  public Repetitions(List<Parameters> params) {
    this.params = params;
  }

  public List<Parameters> getParams() {
    return params;
  }
}
