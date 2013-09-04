package tn.common.data;

import tn.common.fs.Chunk;

/**
 * 
 * Any implementation of this interface provides some statistical method on
 * underlying operation or resource it encapsulates. For example in the case of
 * a {@link Chunk} and if the underlying implementation is text file then
 * process-able implementation could report stats in terms of chars or strings
 * so far processed and so on.<br>
 * <br>
 * 
 * a process-able unit can be of any of the following states <br>
 * 
 * [READY,IN_PROGRESS,FINISHED,ERRORED]
 * 
 * @author Senthu Sivasambu
 * 
 */
public interface Processable {

	public static enum ProcessState {

		READY("Read", 0), IN_PROGRESS("In Progress", 2), FINISHED("Finished", 4), ERRORED(
				"Error occured", 8);

		public String msg;
		public String statusCode;
		public String auxMsg;

		ProcessState(String msg, int statusCode) {
			this.msg = msg;
			this.auxMsg = "";
		}

		@Override
		public String toString() {
			return "[TNS" + statusCode + "] " + msg + ":" + auxMsg;
		}
	}

	public double total();

	public double remaining();

	public double sofar();

	/**
	 * a round down approximation of progress so far as percentage
	 * 
	 * @return - a round down percentage value
	 */
	public int progress();

	public ProcessState processStatus();

}
