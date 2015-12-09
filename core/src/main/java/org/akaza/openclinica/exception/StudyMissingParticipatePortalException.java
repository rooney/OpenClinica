package org.akaza.openclinica.exception;

/**
 * @author fahri
 *
 * When active study does not have its participate portal or it cannot be reached.
 */
public class StudyMissingParticipatePortalException extends Exception {
    public StudyMissingParticipatePortalException() {
        super();
    }

    public StudyMissingParticipatePortalException(String message) {
        super(message);
    }

    public StudyMissingParticipatePortalException(String message, Throwable cause) {
        super(message, cause);
    }

    public StudyMissingParticipatePortalException(Throwable cause) {
        super(cause);
    }
}
