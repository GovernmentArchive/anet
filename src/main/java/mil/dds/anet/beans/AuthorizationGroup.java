package mil.dds.anet.beans;

import java.util.List;
import java.util.Objects;

import mil.dds.anet.AnetObjectEngine;
import mil.dds.anet.graphql.GraphQLFetcher;
import mil.dds.anet.graphql.GraphQLIgnore;
import mil.dds.anet.utils.Utils;
import mil.dds.anet.views.AbstractAnetBean;

public class AuthorizationGroup extends AbstractAnetBean {

	public static enum AuthorizationGroupStatus { ACTIVE, INACTIVE }

	private String name;
	private String description;
	private List<Position> positions;
	private List<Report> reports;
	private AuthorizationGroupStatus status;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Utils.trimStringReturnNull(name);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = Utils.trimStringReturnNull(description);
	}

	@GraphQLFetcher("positions")
	public List<Position> loadPositions() {
		if (positions == null) {
			positions = AnetObjectEngine.getInstance().getAuthorizationGroupDao().getPositionsForAuthorizationGroup(this);
		}
		return positions;
	}

	@GraphQLIgnore
	public List<Position> getPositions() {
		return positions;
	}

	public void setPositions(List<Position> positions) {
		this.positions = positions;
	}

	@GraphQLFetcher("reports")
	public List<Report> loadReports() {
		if (reports == null) {
			reports = AnetObjectEngine.getInstance().getAuthorizationGroupDao().getReportsForAuthorizationGroup(this);
		}
		return reports;
	}

	public List<Report> getReports() {
		return reports;
	}

	public void setReports(List<Report> reports) {
		this.reports = reports;
	}

	public AuthorizationGroupStatus getStatus() {
		return status;
	}

	public void setStatus(AuthorizationGroupStatus status) {
		this.status = status;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}
		AuthorizationGroup a = (AuthorizationGroup) o;
		return Objects.equals(a.getId(), id)
				&& Objects.equals(a.getName(), name)
				&& Objects.equals(a.getDescription(), description)
				&& Objects.equals(a.getPositions(), positions)
				&& Objects.equals(a.getStatus(), status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, description, positions, status);
	}

	@Override
	public String toString() {
		return String.format("(%d) - %s", id, name);
	}

}
