package org.jsoupit.test.infra;

import java.util.Arrays;
import java.util.List;

import org.jsoupit.Configuration;
import org.jsoupit.Context;
import org.jsoupit.template.ClasspathTemplateResolver;
import org.jsoupit.template.snippet.resolve.DefaultSnippetResolver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class BaseTest {
    private final static Configuration configuration = new Configuration() {
        {
            ClasspathTemplateResolver templateResolver = new ClasspathTemplateResolver();
            List<String> templateBaseFolders = Arrays.asList("/org/jsoupit/test/templates");
            templateResolver.setSearchPathList(templateBaseFolders);
            this.setTemplateResolver(templateResolver);

            DefaultSnippetResolver snippetResolver = new DefaultSnippetResolver();
            List<String> snippetBasePackages = Arrays.asList("org.jsoupit.test");
            snippetResolver.setSearchPathList(snippetBasePackages);
            this.setSnippetResolver(snippetResolver);
        }
    };

    @BeforeMethod
    public void initContext() {
        Context context = Context.getCurrentThreadContext();
        if (context == null) {
            context = new Context();
            context.setConfiguration(configuration);
            Context.setCurrentThreadContext(context);

        }
        context.clearSavedData();
    }

    @AfterMethod
    public void clearContext() {
        Context.getCurrentThreadContext().clearSavedData();
    }

}
