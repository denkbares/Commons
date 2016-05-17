package com.denkbares.semanticcore.changelog;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openrdf.model.Statement;

import de.d3web.strings.Strings;

public class Change {

	public enum Action {
		ADD(), REMOVE()
	}

	private final Action action;
	private final String subject;
	private final String predicate;
	private final String object;
	private final String user;
	private final Date date;

	private static final DateFormat LEGACY_DF = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss:SS");

	public static Change parseCSV(String changeLine) throws ParseException {
		String[] elements = changeLine.split(";");
		Action action = elements[0].equals("ADD") ? Action.ADD : Action.REMOVE;
		Date date = readDate(elements[1]);
		String user = elements[2];
		String subject = elements[3];
		String predicate = elements[4];
		StringBuilder object = new StringBuilder();
		for (int i = 5; i < elements.length; i++) {
			if (i > 5) {
				object.append(";");
			}
			object.append(elements[i]);
		}
		return new Change(action, date, user, subject, predicate, object.toString());
	}

	private static Date readDate(String element) throws ParseException {
		try {
			return Strings.readDate(element);
		}
		catch (ParseException e) {
			return LEGACY_DF.parse(element);
		}
	}

	private Change(Action action, Date date, String user, String subject, String predicate, String object) {
		this.action = action;
		this.date = date;
		this.user = user;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public Change(Action action, Statement statement) {
		this(action, statement.getSubject().stringValue(), statement.getPredicate().stringValue(), statement.getObject()
				.stringValue());
	}

	public Change(Action action, String subject, String predicate, String object) {
		if (action == null || subject == null || predicate == null || object == null) {
			throw new NullPointerException();
		}
		if (subject.isEmpty() || predicate.isEmpty() || object.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.action = action;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.user = System.getProperty("user.name");
		this.date = new Date();
	}

	public String getSubject() {
		return subject;
	}

	public String getPredicate() {
		return predicate;
	}

	public String getObject() {
		return object;
	}

	public String getUser() {
		return user;
	}

	public Date getDate() {
		return date;
	}

	public Action getAction() {
		return action;
	}

	public String toCSVString() {
		StringBuilder builder = new StringBuilder();
		builder.append(action == Action.ADD ? "ADD" : "REMOVE");
		builder.append(";");
		builder.append(Strings.writeDate(date));
		builder.append(";");
		builder.append(user);
		builder.append(";");
		builder.append(subject);
		builder.append(";");
		builder.append(predicate);
		builder.append(";");
		builder.append(object);
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Change other = (Change) obj;
		if (date == null) {
			if (other.date != null) {
				return false;
			}
		}
		else if (!date.equals(other.date)) {
			return false;
		}
		if (object == null) {
			if (other.object != null) {
				return false;
			}
		}
		else if (!object.equals(other.object)) {
			return false;
		}
		if (predicate == null) {
			if (other.predicate != null) {
				return false;
			}
		}
		else if (!predicate.equals(other.predicate)) {
			return false;
		}
		if (subject == null) {
			if (other.subject != null) {
				return false;
			}
		}
		else if (!subject.equals(other.subject)) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		}
		else if (!user.equals(other.user)) {
			return false;
		}
		return true;
	}
}