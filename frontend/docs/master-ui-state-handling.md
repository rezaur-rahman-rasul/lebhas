# Master UI State Handling

Master pages treat empty backend data as a normal loaded state. Initial load failures render inline errors. Toasts are reserved for user-triggered actions such as retry, save, delete, upload, and test connection.

Sidebar active state is router-driven. Usage tabs subscribe to route data so switching between Master usage routes updates content on the first navigation.
