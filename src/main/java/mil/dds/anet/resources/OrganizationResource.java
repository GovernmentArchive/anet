package mil.dds.anet.resources;

import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.dropwizard.auth.Auth;
import mil.dds.anet.AnetObjectEngine;
import mil.dds.anet.beans.ApprovalStep;
import mil.dds.anet.beans.Group;
import mil.dds.anet.beans.Organization;
import mil.dds.anet.beans.Organization.OrganizationType;
import mil.dds.anet.beans.Person;
import mil.dds.anet.beans.Poam;
import mil.dds.anet.beans.search.OrganizationSearchQuery;
import mil.dds.anet.database.OrganizationDao;
import mil.dds.anet.graphql.GraphQLFetcher;
import mil.dds.anet.graphql.GraphQLParam;
import mil.dds.anet.graphql.IGraphQLResource;
import mil.dds.anet.utils.AuthUtils;
import mil.dds.anet.utils.DaoUtils;
import mil.dds.anet.utils.ResponseUtils;
import mil.dds.anet.utils.Utils;

@Path("/api/organizations")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class OrganizationResource implements IGraphQLResource {

	private OrganizationDao dao;
	private AnetObjectEngine engine;
	
	public OrganizationResource(AnetObjectEngine engine) {
		this.dao = engine.getOrganizationDao(); 
		this.engine = engine;
	}
	
	@Override
	public Class<Organization> getBeanClass() { return Organization.class; }
	
	@Override
	public String getDescription() { return "Organizations"; } 
	
	@GET
	@GraphQLFetcher
	@Path("/")
	public List<Organization> getAllOrgs(@DefaultValue("0") @QueryParam("pageNum") Integer pageNum, @DefaultValue("100") @QueryParam("pageSize") Integer pageSize) {
		return dao.getAll(pageNum, pageSize);
	} 

	@GET
	@GraphQLFetcher
	@Path("/topLevel")
	public List<Organization> getTopLevelOrgs(@QueryParam("type") OrganizationType type) { 
		return dao.getTopLevelOrgs(type);
	}
	
	@POST
	@Path("/new")
	@RolesAllowed("ADMINISTRATOR")
	public Organization createNewOrganization(Organization org, @Auth Person user) {
		AuthUtils.assertAdministrator(user); 
		Organization created = dao.insert(org);
		
		if (org.getPoams() != null) { 
			//Assign all of these poams to this organization. 
			for (Poam p : org.getPoams()) { 
				engine.getPoamDao().setResponsibleOrgForPoam(p, created);
			}
		}
		if (org.getApprovalSteps() != null) { 
			//Create the approval steps 
			for (ApprovalStep step : org.getApprovalSteps()) { 
				step.setAdvisorOrganizationId(created.getId());
				Group createdGroup = engine.getGroupDao().insert(step.getApproverGroup());
				step.setApproverGroup(createdGroup);
				engine.getApprovalStepDao().insertAtEnd(step);
			}
		}
		
		return created; 
	}
	
	@GET
	@GraphQLFetcher
	@Path("/{id}")
	public Organization getById(@PathParam("id") int id) {
		return dao.getById(id);
	}
	
	@POST
	@Path("/update")
	@RolesAllowed("SUPER_USER")
	public Response updateOrganization(Organization org, @Auth Person user) { 
		//Verify correct Organization 
		AuthUtils.assertSuperUserForOrg(user, org);
		
		int numRows = dao.update(org);
		
		if (org.getPoams() != null || org.getApprovalSteps() != null) {
			Organization existing = dao.getById(org.getId());
			
			if (org.getPoams() != null) {
				Utils.addRemoveElementsById(existing.loadPoams(), org.getPoams(), 
						newPoam -> engine.getPoamDao().setResponsibleOrgForPoam(newPoam, existing), 
						oldPoamId -> engine.getPoamDao().setResponsibleOrgForPoam(Poam.createWithId(oldPoamId), null));
			}
			if (org.getApprovalSteps() != null) {
				List<ApprovalStep> existingSteps = existing.loadApprovalSteps();
				// for each step we were given
				for (ApprovalStep step : org.getApprovalSteps()) {
					step.setAdvisorOrganizationId(org.getId());
					if (step.getId() != null) { 
						// if it has an ID then it already exists, so check the group to update name or members. 
						ApprovalStep existingStep = Utils.getById(existingSteps, step.getId());
						updateGroup(step.getApproverGroup(), existingStep.loadApproverGroup());
					} else {
						step.setApproverGroup(engine.getGroupDao().insert(step.getApproverGroup()));
						step = engine.getApprovalStepDao().insert(step);
					}
				}
				
				//Fix all the orders.
				//I know this is is inefficient in that it pushes an update to every step
				//TODO: make this more efficient. 
				for (int i=0;i<org.getApprovalSteps().size();i++) { 
					ApprovalStep curr = org.getApprovalSteps().get(i);
					ApprovalStep next = (i == (org.getApprovalSteps().size() -1)) ? null : org.getApprovalSteps().get(i+1);
					curr.setNextStepId(DaoUtils.getId(next));
					engine.getApprovalStepDao().update(curr);
				}
			}
		}
		
		return (numRows == 1) ? Response.ok().build() : Response.status(Status.NOT_FOUND).build();
	}
	
	//Helper method that diffs the name/members of a group and updates them. 
	private void updateGroup(Group newGroup, Group oldGroup) {
		newGroup.setId(oldGroup.getId()); //Always want to make changes to the existing group
		if (newGroup.getName().equals(oldGroup.getName()) == false) { 
			engine.getGroupDao().update(newGroup);
		}
	
		Utils.addRemoveElementsById(oldGroup.getMembers(), newGroup.getMembers(), 
				newPerson -> engine.getGroupDao().addPersonToGroup(newGroup, newPerson), 
				oldPersonId -> engine.getGroupDao().removePersonFromGroup(newGroup, Person.createWithId(oldPersonId)));
	}
	
	@POST
	@GraphQLFetcher
	@Path("/search")
	public List<Organization> search(@GraphQLParam("query") OrganizationSearchQuery query ) {
		return dao.search(query);
	}
	
	@GET
	@Path("/search")
	public List<Organization> search(@Context HttpServletRequest request) {
		try { 
			return search(ResponseUtils.convertParamsToBean(request, OrganizationSearchQuery.class));
		} catch (IllegalArgumentException e) { 
			throw new WebApplicationException(e.getMessage(), e.getCause(), Status.BAD_REQUEST);
		}
	}
	
	@GET
	@Path("/{id}/children")
	public List<Organization> getChildren(@PathParam("id") Integer id) { 
		return dao.getByParentOrgId(id, null);
	}
	
	@GET
	@Path("/{id}/poams")
	public List<Poam> getPoams(@PathParam("id") Integer orgId) { 
		return AnetObjectEngine.getInstance().getPoamDao().getPoamsByOrganizationId(orgId);
	}
}
