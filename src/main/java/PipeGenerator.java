public class PipeGenerator {

    public static void main(String[] args) {
        SnowPipeGenerator snowPipeGenerator = new SnowPipeGenerator();
        args = new String[10];
        args[0]="C:\\Users\\Mohit\\Desktop\\Customer_sf_pipe.xls";
        args[1]="C:\\Users\\Mohit\\Desktop\\Customer_src_mapping.xls";
        args[2]="customer_pipe.sql";
        args[3]="false";
//        java -jar SnowPipeGenerator-1.0-jar-with-dependencies.jar C:\\Users\\Mohit\\Desktop\\DailyOptIn_INF_text_sf_pipe.xls C:\\Users\\Mohit\\Desktop\\DailyOptIn_INF_text_src_mapping.xls  myPipQuery.sql true
        boolean success = snowPipeGenerator.generatePipe(args[0],args[1],args[2],args[3]);
        if (success) {
            System.out.println("SnowPipe Generated Successfully");
        }else {
            System.out.println("Failed to Generated SnowPipe");
        }

    }
}
