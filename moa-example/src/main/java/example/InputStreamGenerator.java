package example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import moa.core.TimingUtils;
import moa.options.ClassOption;
import moa.tasks.MainTask;
import moa.tasks.TaskThread;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

public class InputStreamGenerator {
    public InputStreamGenerator() {
    }

    public void run() throws Exception {
        gradualDrift();
        preprocess();
        realDrifts();
    }


    private void preprocess() throws IOException {
        System.out.println("Commit data preprocessing started!");
        CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader()
                .parse(Files.newBufferedReader(Paths.get("data/raw/mongo.csv")));
        Preprocessor preprocessor = new Preprocessor();
        parser.forEach(preprocessor);
        preprocessor.close();
        parser.close();
        System.out.println("Commit data preprocessing ended!");
    }

    private void realDrifts() throws IOException {
        System.out.println("Real drifts generation started!");
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File("data/preprocessed/mongo.csv"));
        weka.core.Instances data = loader.getDataSet();

        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File("data/stream/mongo.arff"));
        saver.writeBatch();
        System.out.println("Real drifts generation ended!");
    }

    private void gradualDrift() throws Exception {
        System.out.println("Gradual drift generation started!");
        long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        String task =
                "WriteStreamToARFFFile -s (ConceptDriftStream -s (generators.SEAGenerator -f 3) -d (generators.SEAGenerator -f 2) -p 50000 -w 20000)"
                        + " -f data/stream/gradual_concept_drift.arff -m 100000";
        System.out.println(task);
        MainTask currentTask = (MainTask) ClassOption.cliStringToObject(task, MainTask.class, null);
        TaskThread thread = new TaskThread((moa.tasks.Task) currentTask);
        thread.start();
        double time = TimingUtils
                .nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime);
        System.out.println(time + " seconds.");
    }

    public static void main(String[] args) throws Exception {
        InputStreamGenerator generator = new InputStreamGenerator();
        generator.run();
    }
}
