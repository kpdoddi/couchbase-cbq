/**
 * 
 */
package cbq;

import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;

/**
 * @author kpdoddi
 * @version 1.0
 * 
 * This class manages the connection to the cluster and the bucket.
 * Note: For now, only one host name or IP address can be specified.
 *       Bucket open timeout is also hard coded.
 *       
 */
public final class Connection {

	static Cluster cluster = null; //static since we are really dealing with only 1 resource.
	static Bucket bucket = null;

	/**
	 * This function takes in the host name and bucket name and returns the bucket.
	 * The host name could also be specified as IP address with optional port number.
	 * Provision has been made for closing and opening buckets in the same cluster.
	 *   
	 * @param hostname
	 * @param bucketname
	 * @return bucket object
	 */
	static Bucket getBucket (String hostname, String bucketname){
		
		long bucketOpenTimeout = 20;
		/*
		 * Open the cluster.
		 */
		if (cluster == null){
			cluster = CouchbaseCluster.create(hostname);
//			System.out.println("Opened cluster");
		}
		/*
		 * This part opens and closes buckets interactively.
		 */
		if ( bucket != null)
			bucket.close();
		bucket = cluster.openBucket(bucketname,bucketOpenTimeout ,TimeUnit.SECONDS);
//		System.out.println("Opened bucket");
		return bucket;
	}
	
	/**
	 * This function closes the connection.
	 * The open buckets get automatically closed.
	 * 
	 * @return status
	 */
	static boolean closeConnection(){
		if (cluster != null)
			cluster.disconnect();
		return true;
	}
}
