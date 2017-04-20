package cbq;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
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
		/*
		 * Open the cluster and the bucket. We already know the bucket name: travel-sample.
		 */
		Cluster cluster = CouchbaseCluster.create("localhost");
		System.out.println("Opened cluster");
		long bucketOpenTimeout = 20;
		Bucket bucket = cluster.openBucket("travel-sample",bucketOpenTimeout ,TimeUnit.SECONDS);
		System.out.println("Opened bucket");
		
		Scanner in = new Scanner(System.in); // To read from command line.
		StringBuffer sb = new StringBuffer();

		/*
		 * OK. Lets start by printing out the welcome and brief usage.
		 */
		System.out.println(" Welcome to Couchbase Interactive Query Tool.");
		System.out.println(" Note: You can split your N1QL by whitespace or newlines, but has to terminate with ;");
		System.out.println(" Note: You can specify the output format of the query as either Row or Json");
		System.out.println(" Note: Please use backticks if you have any hyphens. Example: select * from `travel-sample` limit 10");
		System.out.println(" At prompt Enter: N1QL query || --json/--row || --quit");
		System.out.print("cbq>");

		boolean inCbq = true; 
		do {
			sb.setLength(0);
			while (in.hasNextLine()){
				String line = in.nextLine();
				/*
				 * OK. Process the line and add to buffer, if not key word.
				 * Note: Right now the user can type --json in the middle of building a query.
				 *       If its on a separate line, then, no problem. 
				 *       If its embedded within the line, then the query malforms.
				 */
				if (line.length() > 0){
					inCbq = line.contains("--quit") ? false:true;
					if (line.contains("--json")){
						System.out.print("Set output to JSON format\ncbq>");
						Pprint.ppType = 1;
						continue;
					}
					if (line.contains("--row")){
						System.out.print("Set output to ROW format\ncbq>");
						Pprint.ppType = 0;
						continue;
					}
					sb.append(" ").append(line.trim());
					if (line.charAt(line.length() -1) == ';' || !inCbq)
						break;
					System.out.print("cbq>");
				}
			}
			if (inCbq){
				System.out.println(sb.toString());
				N1qlQueryResult result = runQuery(bucket,sb);
				if (result != null) // Will return null if problem with query.
					Pprint.pprint(result);
				System.out.print("cbq>");
			}
		} while (inCbq); // Loop till user quits explicitly. Discourage Ctrl-C.
		/*
		 * Close resources here.
		 * Cluster disconnect should automatically close bucket, but let's do this explicitly as best practice.
		 */
		in.close();
		bucket.close();
		cluster.disconnect();
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
	static N1qlQueryResult runQuery(Bucket bucket, StringBuffer sb) throws InterruptedException{
		
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
		    	Thread.sleep(BASE_DELAY * current_attempts);
		    }
		} while (++current_attempts != MAX_RETRIES);
		if (!result.finalSuccess()){ // Catch and print error.
			for (JsonObject err : result.errors()){
				System.out.println("     Error. " + err.toString()); // No fancy formatting. Plain row looks fine.
				return null;
			}
		}
		return result;
	}
}
