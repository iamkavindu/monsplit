# MonSplit

A web-based expense split and sharing app.

## What This Application Does

The application allows users to create user groups and share expenses amongst them.
This is particularly useful for those with a large number of friends, at an office setting, or any other gathering.

## Business Scenario

**Example:** A group of friends is splitting expenses at a restaurant. They want to share the bill amongst themselves.
Then, a one friend (a user) pays the bill.

The application allows the user to:
1. Create a group of friends (friends are app users).
2. Create an expense for the group mentioning the bill amount.
3. Track who owes what.
4. Remind the whole group or a specific friend about the owed amount.
5. Notify the user when each friend paid the owed amount.
6. Notify the user when the whole expense is paid.
7. Manage the group while the bill is still unpaid.
    
   a. Add or remove friends from the group.

   b. Choose whether to cancel or recalculate based on adding or removing friends.

8. View the history of expenses and payments.
9. When added as a friend to an expense group, the friends will be notified in their respective dashboards once they acknowledge the expense.

## Technical Details
* **Framework:** Spring Boot with Spring Modulith + Vaadin Flow
* **Database:** PostgreSQL
* **Authentication:** OAuth2 

## License
GNU GENERAL PUBLIC LICENSE 3.0 – See [LICENSE](LICENSE) file for details.
