package cbq;

import java.util.Scanner;
import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;

/**
 * @author kpdoddi
 * @version 1.0
 *
 */
public class Cbq {

	public static void main(String[] args) throws InterruptedException, JsonProcessingException {
		
		String hostname = "localhost";
		String bucketname = "travel-sample";
		Bucket bucket = null;
		
		if (args.length != 2){
			System.out.println("Usage: cbq hostname[:port] bucketname");
			System.exit(0);
		} else {
			hostname = args[0];
			bucketname = args[1];
			bucket = Connection.getBucket(hostname, bucketname);
		}

		Scanner in = new Scanner(System.in); // To read from command line.
		StringBuffer sb = new StringBuffer();

		/*
		 * OK. Lets start by printing out the welcome and brief usage.
		 */
		System.out.println("    Welcome to Couchbase Interactive Query Tool.");
		System.out.println("    You can split your N1QL by whitespace or newlines, but has to terminate with ;");
		System.out.println("    You can specify the output format of the query as either Row or Json");
		System.out.println("    Please use backticks if you have any hyphens. Example: select * from `travel-sample` limit 10");
		System.out.println("    At prompt Enter: N1QL query || --json/--row || --quit");
		System.out.print("cbq>");

		boolean inCbq = true; 
		do {
			sb.setLength(0);
			while (in.hasNextLine()){
				String line = in.nextLine().trim();
				/*
				 * OK. Process the line and add to buffer, if not key word.
				 */
				if (line.length() > 0){
					if (line.startsWith("--")){
						if ((inCbq = processDdash(line)))
							continue;
						else
							break;
					} else {
						sb.append(" ").append(line); // keep building the query
						if (line.charAt(line.length() -1) == ';' || !inCbq) //OK. User is finished with query.
							break;
					}
				}
				System.out.print("cbq>");
			}
			if (inCbq){
				System.out.println("    SQL:" + sb.toString());
				N1qlQueryResult result = runQuery(bucket,sb);
				if (result != null) // Will return null if problem with query.
					Pprint.pprint(result);
				System.out.print("cbq>");
			}
		} while (inCbq); // Loop till user quits explicitly. Discourage Ctrl-C.
		/*
		 * Close resources here.
		 * Cluster disconnect should automatically close bucket.
		 */
		in.close();
		Connection.closeConnection();
	}
	
	/**
	 * This function runs the actual query.
	 * It handles the Temporary Failure Error by retrying the query.
	 * This error is received when the server is temporarily inhibiting an error that doesn't allow it to respond successfully.
	 * It checks for any errors and prints them out to the console.
	 * Note: Handle only Transient Errors with retry. Throw all other errors and restart the application.
	 *       The Interrupted Exception is thrown by the thread, nothing to do with server.
	 * 
	 * 
	 * @param bucket
	 * @param sb
	 * @return result
	 * @throws InterruptedException
	 */
	static N1qlQueryResult runQuery(Bucket bucket, StringBuffer sb) throws InterruptedException
	{
		
		int MAX_RETRIES = 5; // Maybe make these configurable by the user from command prompt. V.next.
		int BASE_DELAY = 50; // milliseconds
		N1qlQuery q = N1qlQuery.simple(sb.toString());
		N1qlQueryResult result = null;
		
		int current_attempts = 1;
		do {
		    try {
				result = bucket.query(q);
		        break;
		    } catch (TemporaryFailureException error) {
		    	System.out.println("    Temporary Failure: Will try again in " + BASE_DELAY * current_attempts + "ms");
		    	Thread.sleep(BASE_DELAY * current_attempts);
		    }
		} while (++current_attempts != MAX_RETRIES);
		if (!result.finalSuccess()){ // Catch and print error.
			for (JsonObject err : result.errors()){
				System.out.println("    Error: " + err.toString()); // No fancy formatting. Plain row looks fine.
				return null;
			}
		}
		System.out.println("    Status: " + result.status());
		return result;
	}

	/**
	 * This function handles the line starting with the double-dash --.
	 * 
	 * @param line
	 * @return true if line != --quit
	 */
	static boolean processDdash(String line)
	{
//		String[] li = line.toLowerCase().split("=");
		if (line.contains("--quit")){
			return false;
		}
		if (line.contains("--json")){
			System.out.print("    Set output to JSON format\ncbq>");
			Pprint.ppType = 1;
		}
		else if (line.contains("--row")){
			System.out.print("    Set output to ROW format\ncbq>");
			Pprint.ppType = 0;
		}
		else {
			System.out.print("\ncbq>"); // treat this as a comment and carry on
		}
		return true;
	}
}
