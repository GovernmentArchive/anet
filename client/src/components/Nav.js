import React, {Component, PropTypes} from 'react'
import {Nav, NavItem, NavDropdown, MenuItem} from 'react-bootstrap'
import {IndexLinkContainer as Link} from 'react-router-bootstrap'
import LinkTo from 'components/LinkTo'
import moment from 'moment'

import {Organization} from 'models'

export default class extends Component {
	static contextTypes = {
		app: PropTypes.object.isRequired,
	}

	render() {
		let appData = this.context.app.state
		let currentUser = appData.currentUser
		let organizations = appData.organizations || []

		return (
			<Nav bsStyle="pills" stacked>
				<Link to="/">
					<NavItem>Home</NavItem>
				</Link>

				{currentUser && <Link to={"search?type=reports&authorId=" + currentUser.id}>
					<NavItem>My Reports</NavItem>
				</Link>
				}

				<NavDropdown title="Organizations" id="organizations">
					{Organization.map(organizations, org =>
						<LinkTo organization={org} componentClass={Link} key={org.id}>
							<MenuItem>{org.shortName}</MenuItem>
						</LinkTo>
					)}
				</NavDropdown>

				<Link to="/rollup">
					<NavItem>Daily Rollup</NavItem>
				</Link>

				{currentUser.isAdmin() &&
					<Link to={"/search?type=reports&pageSize=100&engagementDateStart=" + moment().add(1, 'days').hour(0).valueOf() } >
						<NavItem>Future Engagements</NavItem>
					</Link>
				}

				{process.env.NODE_ENV === 'development' &&
					<Link to="/graphiql">
						<NavItem>GraphQL</NavItem>
					</Link>
				}

				{currentUser.isAdmin() &&
					<Link to="/admin">
						<NavItem>Admin</NavItem>
					</Link>
				}
			</Nav>
		)
	}
}
