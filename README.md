# SnowPipeGenerator
1. Command structure:
   java -jar SnowPipeGenerator.jar <path/to/pipe.xls> <path/to/src_mapping.xls>  <PipeQuery.sql> <createInDb (true|false)>
   
2. If want to create in DB update dbConnection.properties<username>,<password>,<account> else ignore and pass false.

3. Example Command:

// This will also save SnowPipe in DB
java -jar SnowPipeGenerator.jar C:\\Users\\Mohit\\Desktop\\DailyOptIn_INF_text_sf_pipe.xls C:\\Users\\Mohit\\Desktop\\DailyOptIn_INF_text_src_mapping.xls  myPipQuery.sql true


// This will generate Sql file only
java -jar SnowPipeGenerator.jar C:\\Users\\Mohit\\Desktop\\DailyOptIn_INF_text_sf_pipe.xls C:\\Users\\Mohit\\Desktop\\DailyOptIn_INF_text_src_mapping.xls  myPipQuery.sql false
