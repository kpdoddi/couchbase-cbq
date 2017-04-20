package cbq;

import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.transcoder.JacksonTransformers;


/**
 * @author kpdoddi
 * @version 1.0
 * 
 * This class prints the rows from the ResultSet to the console, as either plain Rows 
 * or Pretty Prints as JSON. It also prints out the execution information (like Couchbase Console).
 * 
 * Input: The non-null result object.
 * Output: Prints rows to Console
 * Returns: void
 *
 */
public final class Pprint {
	public static int ppType = 0; // This will be set by the user to either 0-ROW or 1-JSON from the interactive tool

	/**
	 * This function expects a non-null result object. We have taken care of this elsewhere.
	 * When we get to this function, the result set has been pulled back to client.
	 * The exception which is thrown is if the server returns a malformed JSON object, not expected.
	 * 
	 * @param result
	 * @throws JsonProcessingException
	 */
	public static void pprint(N1qlQueryResult result) throws JsonProcessingException{
		
		/*
		 * Lets print out the result information first
		 */
		System.out.println("Elapsed: " +  result.info().elapsedTime() + 
				" Execution: " + result.info().executionTime() + 
				" Result Count: " + result.info().resultCount() + 
				" Result Size: " + result.info().resultSize());
		
		/*
		 * Next, walk through the rows and print them out
		 */
		for (N1qlQueryRow row : result){
			if (ppType == 0)
				System.out.println(row.toString());
			else
				System.out.println(JacksonTransformers.MAPPER
				        .writerWithDefaultPrettyPrinter() //this will do pretty print
				        .writeValueAsString(row.value()));
		}		
	}
}
