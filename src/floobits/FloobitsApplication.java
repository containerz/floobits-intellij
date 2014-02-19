package floobits;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import floobits.common.*;
import floobits.dialogs.CreateAccount;
import floobits.utilities.Flog;
import floobits.utilities.SelectFolder;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.net.URI;

public class FloobitsApplication implements ApplicationComponent {
    public static FloobitsApplication self;
    private Boolean createAccount = true;

    public FloobitsApplication() {
        self = this;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    public synchronized void projectOpened(FlooContext context) {
        PersistentJson p = PersistentJson.getInstance();
        if (createAccount && !new Settings(context).isComplete() && (p == null || !p.disable_account_creation)) {
            createAccount = false;
            CreateAccount createAccount1 = new CreateAccount(context);
            createAccount1.createCenterPanel();
            createAccount1.show();
        }
    }

    @NotNull
    public String getComponentName() {
        return "FloobitsApplication";
    }

    public void joinWorkspace(FlooContext context, final FlooUrl flooUrl, String location) {
        if (!API.workspaceExists(flooUrl, context)) {
            context.error_message(String.format("The workspace %s does not exist!", flooUrl.toString()));
            return;
        }
        Project projectForPath = getProject(location);
        if (projectForPath == null) {
            if (context.isJoined()) {
                projectForPath = createProject(location, flooUrl.workspace);
                context = FloobitsPlugin.getInstance(projectForPath).context;
            }
        } else if (projectForPath != context.project) {
            context = FloobitsPlugin.getInstance(projectForPath).context;
        }
        Window window = WindowManager.getInstance().suggestParentWindow(context.project);
        if (window != null) {
            window.toFront();
        }
        context.joinWorkspace(flooUrl, location, false);
    }

    public void joinWorkspace(final FlooContext context, final String url) {
        final FlooUrl f;

        try {
            f = new FlooUrl(url);
        } catch (Exception e) {
            context.error_message(String.format("Invalid url: %s", e));
            return;
        }

        PersistentJson persistentJson = PersistentJson.getInstance();
        Workspace workspace;
        try {
            workspace = persistentJson.workspaces.get(f.owner).get(f.workspace);
        } catch (Exception e) {
            workspace = null;
        }
        if (workspace != null) {
            joinWorkspace(context, f, workspace.path);
            return;
        }
        FlooUrl flooUrl = DotFloo.read(context.project.getBasePath());
        if (flooUrl != null) {
            URI uri = URI.create(flooUrl.toString());
            URI normalizedURL = URI.create(url);
    
            if (uri.getPath().equals(normalizedURL.getPath())) {
                joinWorkspace(context, flooUrl, context.project.getBasePath());
                return;
            }
        }

        SelectFolder.build(new RunLater<String>() {
            @Override
            public void run(String path) {
                joinWorkspace(context, f, path);
            }
        });
    }

    private Project getProject(String path) {
        ProjectManager pm = ProjectManager.getInstance();
        // Check open projects
        Project[] openProjects = pm.getOpenProjects();
        for (Project project : openProjects) {
            if (path.equals(project.getBasePath())) {
                return project;
            }
        }

        // Try to open existing project
        try {
            return pm.loadAndOpenProject(path);
        } catch (Exception e) {
            Flog.warn(e);
        }

        return null;
    }
    private Project createProject(String path, String name) {
        ProjectManager pm = ProjectManager.getInstance();
        // Create project
        Project project = pm.createProject(name, path);
        try {
            ProjectManager.getInstance().loadAndOpenProject(path);
        } catch (Exception e) {
            Flog.warn(e);
        }
        return project;
    }
}
