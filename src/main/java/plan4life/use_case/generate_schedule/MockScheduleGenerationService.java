package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;
import java.util.Map;

/**
 * Simple mock generator used for local testing until the real LLM/RAG service is integrated.
 * It preserves fixed activities and randomly fills a few more slots using Schedule.populateRandomly()
 * (but then overwrites any conflicts with the fixed ones).
 */
public class MockScheduleGenerationService implements ScheduleGenerationService {

    @Override
    public Schedule generate(String routineDescription, Map<String, String> fixedActivities) {
        Schedule s = new Schedule();

        // Seed with random or default content
        s.populateRandomly();

        // Overwrite/add fixed activities from request (fixedActivities may use your chosen key format)
        if (fixedActivities != null) {
            for (Map.Entry<String, String> e : fixedActivities.entrySet()) {
                s.addActivity(e.getKey(), e.getValue());
            }
        }
        return s;
    }
}

