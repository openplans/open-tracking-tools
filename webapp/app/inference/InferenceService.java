package inference;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import models.InferenceInstance;

import org.openplans.tools.tracking.impl.Observation;

import play.mvc.Result;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * This class is an Actor that responds to LocationRecord messages and processes.
 * Note: this is essentially a thread(instance)
 * 
 * @author bwillard
 * 
 */
public class InferenceService extends UntypedActor {

  private static final Map<String, InferenceInstance> vehicleToInstance = Maps
      .newConcurrentMap();

  private static final Multimap<String, InferenceResultRecord> vehicleToTraceResults = LinkedHashMultimap
      .create();

  private final LoggingAdapter log = Logging.getLogger(getContext().system(),
      this);

  public static void clearInferenceData() {
    vehicleToInstance.clear();
    vehicleToTraceResults.clear();
  }
  
  public static void processRecord(Observation observation) {

    final InferenceInstance ie = getInferenceInstance(observation
        .getVehicleId());

    ie.update(observation);

    final InferenceResultRecord infResult = InferenceResultRecord
        .createInferenceResultRecord(observation, ie);

    vehicleToTraceResults.put(observation.getVehicleId(), infResult);

  }
  
  @Override
  public void onReceive(Object location) throws Exception {
    synchronized (this) {
      if (location instanceof Observation) {
        final Observation observation = (Observation) location;
        processRecord(observation);
        
        log.info("Message received:  "
            + observation.getTimestamp().toString());
      }
    }

  }

  public static InferenceInstance getInferenceInstance(String vehicleId) {
    InferenceInstance ie = vehicleToInstance.get(vehicleId);

    if (ie == null) {
      ie = new InferenceInstance(vehicleId);
      vehicleToInstance.put(vehicleId, ie);
    }

    return ie;
  }

  public static Collection<InferenceResultRecord> getTraceResults(
    String vehicleId) {
    return vehicleToTraceResults.get(vehicleId);
  }

  public static void addSimulationRecords(String simulationName,
    List<InferenceResultRecord> results) {
    vehicleToTraceResults.putAll(simulationName, results);
    // TODO this is lame.
    InferenceInstance instance = new InferenceInstance(simulationName);
    instance.recordsProcessed = results.size();
    vehicleToInstance.put(simulationName, instance);
  }

  public static List<InferenceInstance> getInferenceInstances() {
    return Lists.newArrayList(vehicleToInstance.values());
  }

  public static void remove(String name) {
    
    vehicleToInstance.remove(name);
    vehicleToTraceResults.removeAll(name);
    
  }

}
