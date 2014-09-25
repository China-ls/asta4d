package com.astamuse.asta4d.web.test.form;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.astamuse.asta4d.web.WebApplicationContext;
import com.astamuse.asta4d.web.form.annotation.CascadeFormField;
import com.astamuse.asta4d.web.form.annotation.Form;
import com.astamuse.asta4d.web.form.annotation.renderable.Input;
import com.astamuse.asta4d.web.form.annotation.renderable.InputHidden;
import com.astamuse.asta4d.web.form.flow.base.FormFlowConstants;
import com.astamuse.asta4d.web.form.flow.classical.MultiStepFormFlowHandler;
import com.astamuse.asta4d.web.form.validation.FormValidationMessage;
import com.astamuse.asta4d.web.test.WebTestBase;

public class MultiStepFormHandlerTest extends WebTestBase {

    private static final String FAKE_TRACE_MAP_ID = "FAKE_TRACE_MAP_ID";

    private static Map<String, Object> savedTraceMap = null;

    private static TestForm savedForm = null;

    @Form
    public static class SubForm {
        @Input
        @NotNull
        private Integer subData;
    }

    @Form
    public static class SubArray {

        @Input(name = "year-@")
        @Max(2000)
        private Integer year;
    }

    @Form
    public static class SubArray2 {

        @Input(name = "age-@")
        @Max(100)
        private Integer age;
    }

    @Form
    public static class TestForm {

        @InputHidden
        @Max(30)
        private Integer id;

        @Input
        @NotBlank
        private String data;

        @CascadeFormField
        @Valid
        private SubForm subForm;

        @CascadeFormField(containerSelector = "subArray-container", arrayLengthField = "subArrayLength")
        @NotEmpty
        @Valid
        private SubArray[] subArray;

        @InputHidden
        @NotNull
        private Integer subArrayLength;

        @CascadeFormField(containerSelector = "subArray-container", arrayLengthField = "subArrayLength2")
        @NotEmpty
        @Valid
        private SubArray2[] subArray2;

        @InputHidden
        @NotNull
        private Integer subArrayLength2;
    }

    public static class TestFormHandler extends MultiStepFormFlowHandler<TestForm> {

        private List<FormValidationMessage> msgList = new LinkedList<>();

        public TestFormHandler() {
            super(TestForm.class, "/testform/");
        }

        @Override
        protected void updateForm(TestForm form) {
            savedForm = form;
        }

        @Override
        protected TestForm createInitForm() {
            TestForm form = new TestForm();
            form.subForm = new SubForm();
            form.subArray = new SubArray[0];
            form.subArray2 = new SubArray2[0];
            form.subArrayLength = 0;
            form.subArrayLength2 = 0;
            return form;
        }

        @Override
        protected String saveTraceMap(Map<String, Object> traceMap) {
            savedTraceMap = traceMap;
            return FAKE_TRACE_MAP_ID;
        }

        @Override
        protected Map<String, Object> restoreTraceMap(String data) {
            return savedTraceMap;
        }

        @Override
        protected void clearSavedTraceMap() {
            savedTraceMap = null;
        }

        @Override
        protected void outputValidationMessage(FormValidationMessage msg) {
            msgList.add(msg);
        }

        public void assertMessageSize(int expectedSize) {
            try {
                Assert.assertEquals(msgList.size(), expectedSize);
            } catch (AssertionError e) {
                throw new AssertionError(e.getMessage() + createExistingMessageInfo(), e);
            }
        }

        public void assertMessage(String name, String msgReg) {
            for (FormValidationMessage msg : msgList) {
                if (msg.getName().equals(name) && msg.getMessage().matches(msgReg)) {
                    return;
                }
            }
            String errMsg = "expected msg as name[%s] and msg(reg)[%s] but not found.";
            errMsg = String.format(errMsg, name, msgReg);
            errMsg += createExistingMessageInfo();
            throw new AssertionError(errMsg);
        }

        private String createExistingMessageInfo() {
            String s = "Existing messages:\n";
            for (FormValidationMessage msg : msgList) {
                s += msg.toString() + "\n";
            }
            return s;
        }
    }

    private Enumeration<String> requestParametersEnum(Map<String, String[]> map) {
        return Collections.enumeration(map.keySet());
    }

    private void mockRequestParameter(HttpServletRequest request, Map<String, String[]> map) {
        when(request.getParameterNames()).thenReturn(requestParametersEnum(map));
        for (Entry<String, String[]> entry : map.entrySet()) {
            when(request.getParameterValues(entry.getKey())).thenReturn(entry.getValue());
        }
    }

    @BeforeMethod
    public void before() {
        super.initContext();
        WebApplicationContext context = WebApplicationContext.getCurrentThreadWebApplicationContext();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        context.setRequest(request);
        context.setResponse(response);

        HttpSession session = mock(HttpSession.class);

        when(request.getCookies()).thenReturn(new Cookie[0]);
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getSession(true)).thenReturn(session);
    }

    private void initParams(Map<String, String[]> map) throws Exception {

        WebApplicationContext context = WebApplicationContext.getCurrentThreadWebApplicationContext();
        HttpServletRequest request = context.getRequest();

        mockRequestParameter(request, map);

    }

    private Map<String, String[]> requestParameters_init = new HashMap<String, String[]>() {
        {

        }
    };

    @Test
    public void testInitStep() throws Exception {

        initParams(requestParameters_init);

        TestFormHandler handler = new TestFormHandler();

        Assert.assertEquals(handler.handle(), "/testform/input.html");

    }

    private Map<String, String[]> requestParameters_inputWithoutTraceMap = new HashMap<String, String[]>() {
        {
            put("id", new String[] { "1" });
            put("data", new String[] { "data-content" });
            put("step-current", new String[] { "input" });
            put("step-failed", new String[] { "input" });
            put("step-success", new String[] { "confirm" });
        }
    };

    @Test(dependsOnMethods = "testInitStep")
    public void testInputWithoutTraceMap() throws Exception {

        initParams(requestParameters_inputWithoutTraceMap);

        TestFormHandler handler = new TestFormHandler();

        Assert.assertEquals(handler.handle(), "/testform/input.html");
    }

    private Map<String, String[]> requestParameters_inputWithTypeUnMatchError = new HashMap<String, String[]>() {
        {
            put("id", new String[] { "ss" });
            put("data", new String[] { "data-content" });
            put("subData", new String[] { "sub data" });
            put("year-0", new String[] { "sub array year" });
            put("subArrayLength", new String[] { "1" });
            put("subArrayLength2", new String[] { "0" });
            put("step-current", new String[] { "input" });
            put("step-failed", new String[] { "input" });
            put("step-success", new String[] { "confirm" });
            put(FormFlowConstants.FORM_STEP_TRACE_MAP_STR, new String[] { FAKE_TRACE_MAP_ID });
        }
    };

    private static final String IntegerTypeUnMatch = ".+ is expecting Integer but value\\[.+\\] found\\.";

    @Test(dependsOnMethods = "testInputWithoutTraceMap")
    public void testInputWithTypeUnMatchError() throws Exception {

        initParams(requestParameters_inputWithTypeUnMatchError);

        TestFormHandler handler = new TestFormHandler();

        Assert.assertEquals(handler.handle(), "/testform/input.html");

        handler.assertMessageSize(3);
        handler.assertMessage("id", IntegerTypeUnMatch);
        handler.assertMessage("subData", IntegerTypeUnMatch);
        handler.assertMessage("year-0", IntegerTypeUnMatch);
    }

    private Map<String, String[]> requestParameters_inputWithValueValidationError = new HashMap<String, String[]>() {
        {
            put("id", new String[] { "77" });
            put("data", new String[] { "" });
            // put("subData", new String[] { "sub data" });
            put("year-0", new String[] { "2002" });
            put("year-1", new String[] { "2003" });
            put("subArrayLength", new String[] { "2" });
            put("subArrayLength2", new String[] { "0" });
            put("step-current", new String[] { "input" });
            put("step-failed", new String[] { "input" });
            put("step-success", new String[] { "confirm" });
            put(FormFlowConstants.FORM_STEP_TRACE_MAP_STR, new String[] { FAKE_TRACE_MAP_ID });
        }
    };

    private static final String MsgNotEmpty = ".+ may not be empty";

    private static final String MsgNotNULL = ".+ may not be null";

    private static final String MsgMax = ".+ must be less than or equal to [0-9]+";

    @Test(dependsOnMethods = "testInputWithTypeUnMatchError")
    public void testInputWithValueValidationError() throws Exception {

        initParams(requestParameters_inputWithValueValidationError);

        TestFormHandler handler = new TestFormHandler();

        Assert.assertEquals(handler.handle(), "/testform/input.html");

        handler.assertMessageSize(6);
        handler.assertMessage("id", MsgMax);
        handler.assertMessage("data", MsgNotEmpty);
        handler.assertMessage("subData", MsgNotNULL);
        handler.assertMessage("year-0", MsgMax);
        handler.assertMessage("year-1", MsgMax);
        handler.assertMessage("subArray2", MsgNotEmpty);
    }

    private Map<String, String[]> requestParameters_goToConfirm = new HashMap<String, String[]>() {
        {
            put("id", new String[] { "22" });
            put("data", new String[] { "data-content" });
            put("subData", new String[] { "123" });
            put("year-0", new String[] { "1998" });
            put("year-1", new String[] { "1999" });
            put("subArrayLength", new String[] { "2" });
            put("age-0", new String[] { "88" });
            put("subArrayLength2", new String[] { "1" });
            put("step-current", new String[] { "input" });
            put("step-failed", new String[] { "input" });
            put("step-success", new String[] { "confirm" });
            put(FormFlowConstants.FORM_STEP_TRACE_MAP_STR, new String[] { FAKE_TRACE_MAP_ID });
        }
    };

    @Test(dependsOnMethods = "testInputWithValueValidationError")
    public void testGoToConfirm() throws Exception {

        initParams(requestParameters_goToConfirm);

        TestFormHandler handler = new TestFormHandler();

        Assert.assertEquals(handler.handle(), "/testform/confirm.html");

        handler.assertMessageSize(0);
    }

    private Map<String, String[]> requestParameters_exit = new HashMap<String, String[]>() {
        {
            put("step-exit", new String[] { "exit" });
        }
    };

    @Test(dependsOnMethods = "testGoToConfirm")
    public void testExit() throws Exception {

        initParams(requestParameters_exit);

        TestFormHandler handler = new TestFormHandler();

        Assert.assertNull(handler.handle());
        Assert.assertNull(savedTraceMap);
        handler.assertMessageSize(0);
    }

    @Test(dependsOnMethods = "testExit")
    public void testInitAgain() throws Exception {
        testInitStep();
    }

    @Test(dependsOnMethods = "testInitAgain")
    public void testGoConfirmAgain() throws Exception {
        testGoToConfirm();
    }

    private Map<String, String[]> requestParameters_complete = new HashMap<String, String[]>() {
        {
            put("step-current", new String[] { "confirm" });
            put("step-failed", new String[] { "input" });
            put("step-success", new String[] { "complete" });
            put(FormFlowConstants.FORM_STEP_TRACE_MAP_STR, new String[] { FAKE_TRACE_MAP_ID });
        }
    };

    @Test(dependsOnMethods = "testGoConfirmAgain")
    public void testComplete() throws Exception {

        initParams(requestParameters_complete);

        TestFormHandler handler = new TestFormHandler();

        Assert.assertEquals(handler.handle(), "/testform/complete.html");

        handler.assertMessageSize(0);

        Assert.assertEquals(savedForm.id.intValue(), 22);
        Assert.assertEquals(savedForm.data, "data-content");
        Assert.assertEquals(savedForm.subForm.subData.intValue(), 123);
        Assert.assertEquals(savedForm.subArray.length, 2);
        Assert.assertEquals(savedForm.subArray[0].year.intValue(), 1998);
        Assert.assertEquals(savedForm.subArray[1].year.intValue(), 1999);
        Assert.assertEquals(savedForm.subArray2.length, 1);
        Assert.assertEquals(savedForm.subArray2[0].age.intValue(), 88);
    }
}
