package au.edu.rmit.tzar.parser.beans;

import au.edu.rmit.tzar.DynamicObjectFactory;
import au.edu.rmit.tzar.api.TzarException;
import au.edu.rmit.tzar.runners.mapreduce.MapReduce;
import au.edu.rmit.tzar.runners.mapreduce.Mapper;
import au.edu.rmit.tzar.runners.mapreduce.Reducer;

import java.util.Map;

/**
 * Bean to represent the mapreduce configuration in the project config.
 */
public class MapReduceBean {
  private String mapper_class;
  private Map<String, String> mapper_flags;
  private String reducer_class;
  private Map<String, String> reducer_flags;

  public MapReduce toMapReduce() throws TzarException {
    Mapper mapper = new DynamicObjectFactory<Mapper>().getInstance(Mapper.class.getPackage().getName() + "." +
        mapper_class);
    Reducer reducer = new DynamicObjectFactory<Reducer>().getInstance(Reducer.class.getPackage().getName() + "." +
        reducer_class);
    mapper.setFlags(mapper_flags);
    reducer.setFlags(reducer_flags);
    return new MapReduce(mapper, reducer);
  }

  public static MapReduceBean fromMapReduce(MapReduce mapReduce) {
    MapReduceBean mapReduceBean = new MapReduceBean();
    mapReduceBean.mapper_class = mapReduce.getMapper().getClass().getSimpleName();
    mapReduceBean.reducer_class = mapReduce.getReducer().getClass().getSimpleName();
    mapReduceBean.mapper_flags = mapReduce.getMapper().getFlags();
    mapReduceBean.reducer_flags = mapReduce.getReducer().getFlags();
    return mapReduceBean;
  }
}
