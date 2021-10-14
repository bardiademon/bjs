package com.bardiademon.JavaServer.Server.HttpResponse;

import com.bardiademon.JavaServer.bardiademon.Str;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BjsHtml
{
    private final List <Bjs> imports = new ArrayList <> ();

    private static final String
            REGEX_IMPORT = "import(\\s*|(\r|\n|\r\n)*)((^*[A-Za-z])([a-zA-Z0-9_])*(\\.)*)*;",
            REGEX_METHOD = "^*[A-Za-z][a-zA-Z0-9_]*(\\s*(\r|\n|\r\n)*)\\(\\)(\\s*|(\r|\n|\r\n)*);",
            REGEX_VAR = "(\\s*(\r|\n|\r\n)*)^*[A-Za-z][a-zA-Z0-9_]*;",
            REGEX_FOR = "for\\s*\\(^*[A-Za-z][a-zA-Z0-9_]*\\s*:\\s*^*[A-Za-z][a-zA-Z0-9_]*\\)\\s*(\r\n|\r|\n)*\\{";
    private final String path;

    private final Param[] paramConstructor;
    private List <Param> params;
    private StringBuilder bjsHtml;

    private String html;
    private List <Bjs> bjs;

    public BjsHtml (final String path , final Param... params)
    {
        this.path = path;
        this.paramConstructor = params;
    }

    public void apply () throws Exception
    {
        final File file = new File (path);
        if (file.exists ())
        {
            bjsHtml = new StringBuilder (new String (Files.readAllBytes (file.toPath ()) , StandardCharsets.UTF_8));
            if (bjsHtml.length () > 0)
            {
                this.params = Arrays.asList (paramConstructor);
                bjs = getBjs (bjsHtml.toString ());
                if (bjs.size () > 0) analysis ();
            }
            html = bjsHtml.toString ();
        }
        else throw new IOException ("File not found!");
    }

    private List <Bjs> getBjs (final String html)
    {
        final List <Bjs> bjs = new ArrayList <> ();
        final Pattern compile = Pattern.compile ("<%bjs((\\s*(\r|\n|\r\n)*)(.*?|(\r|\n|\r\n))*(\\s*(\r|\n|\r\n)*))%>");
        Matcher matcher = compile.matcher (html);
        while (matcher.find ())
            bjs.add (new Bjs (matcher.start (0) , matcher.end (0) , matcher.group (1).trim ()));
        return bjs;
    }

    private void analysis () throws Exception
    {
        for (int i = 0, len = this.bjs.size (); i < len; i++)
        {
            final Bjs bjs = this.bjs.get (i);
            if (bjs.value.matches (REGEX_FOR))
            {
                final int bracket = getBracket (i , len , "}");
                if (bracket > 0)
                {
                    final Bjs[] bodyFor = new Bjs[bracket];
                    for (int j = 0; j < bracket; j++) bodyFor[j] = this.bjs.get (i++);
                    final String htmlBodyFor = analysisFor (bodyFor);

                    bjsHtml = new StringBuilder (bjsHtml.replace (bodyFor[0].startIndex , bodyFor[bodyFor.length - 1].endIndex , htmlBodyFor));
                    this.bjs = getBjs (bjsHtml.toString ());

                    analysis ();
                    return;
                }
            }
            else if (bjs.value.matches (REGEX_IMPORT))
            {
                imports.add (bjs);
                bjsHtml = new StringBuilder (bjsHtml.replace (bjs.startIndex , bjs.endIndex , ""));
                this.bjs = getBjs (bjsHtml.toString ());
                analysis ();
                return;
            }
            else
            {
                // agar az on object nabashe baraye barasi be in method bere
                final String invoke = getValueClass (bjs);
                bjsHtml = new StringBuilder (bjsHtml.replace (bjs.startIndex , bjs.endIndex , invoke.trim ()));
                this.bjs = getBjs (bjsHtml.toString ());
                analysis ();
                return;
            }
        }
    }

    private String analysisFor (final Bjs[] bjs) throws Exception
    {
        final Bjs bjsFor = bjs[0];
        final Matcher matcherCircleBracket = Pattern.compile ("\\s*^*[A-Za-z][a-zA-Z0-9_]*(\\s*|(\r|\n|\r\n)*)*:(\\s*(\r|\n|\r\n)*)^*[A-Za-z][a-zA-Z0-9_]*\\s*").matcher (bjsFor.value);
        if (matcherCircleBracket.find ())
        {
            final String bodyCircleBracket = matcherCircleBracket.group (0).trim ();
            final String[] varClassName = bodyCircleBracket.split (":");
            if (varClassName.length == 2)
            {
                final String var = varClassName[0].trim ();
                final String varName = varClassName[1].trim ();
                if (!Str.isEmpty (var) && !Str.isEmpty (varName))
                {
                    final Param param = getParam (varName);
                    if (param != null)
                    {
                        final String htmlForBody = bjsHtml.substring (bjs[0].endIndex , bjs[bjs.length - 1].startIndex).trim ();
                        final StringBuilder newHtmlForBody = new StringBuilder ();

                        final List <Bjs> forBjs = getBjs (htmlForBody);

                        if (param.aClass instanceof List <?> || param.aClass instanceof Object[])
                        {
                            if (param.aClass instanceof List <?>)
                            {
                                final List <?> lst = (List <?>) param.aClass;
                                for (final Object obj : lst)
                                    newHtmlForBody.append (bodyRealFor (bjs , forBjs , new StringBuilder (htmlForBody) , obj , var));
                            }
                            else
                            {
                                final Object[] lst = (Object[]) param.aClass;
                                for (final Object obj : lst)
                                    newHtmlForBody.append (bodyRealFor (bjs , forBjs , new StringBuilder (htmlForBody) , obj , var));
                            }
                            return newHtmlForBody.toString ();
                        }
                        else throw new Exception (varName + " isn't a list");
                    }
                    else throw new Exception ("Undefined param: " + varName + "");
                }
                else throw new Exception ("Invalid for");
            }
            else throw new Exception ("Invalid for");
        }
        else throw new Exception ("Not found circle bracket for");
    }

    private String bodyRealFor (final Bjs[] bjs , List <Bjs> forBjs , final StringBuilder bodyHtml , final Object obj , final String var) throws Exception
    {
        List <Bjs> forBjsTmp = forBjs;
        for (int i = 1; i < bjs.length - 1; i++)
        {
            final Bjs bj = bjs[i];
            // agar in bjs yek method or yek var az in list bashe for (lst : LIST)
            if (isMethod (bj.value , var) || bj.value.matches (var + "(\\s*(\r|\n|\r\n))*\\." + REGEX_VAR))
            {
                final String[] split = bj.value.split ("\\.");
                if (isMethod (bj.value , var))
                {
                    final String methodName = split[1].trim ().split ("\\(\\)")[0].trim ();
                    Object invoke = obj.getClass ().getMethod (methodName).invoke (obj);
                    if (invoke == null) invoke = "null";
                    bodyHtml.replace (forBjsTmp.get (0).startIndex , forBjsTmp.get (0).endIndex , invoke.toString ().trim ());
                    forBjsTmp = new ArrayList <> (getBjs (bodyHtml.toString ()));
                }
                else
                {
                    final String varName = split[1].trim ().split (";")[0].trim ();
                    final Object invoke = obj.getClass ().getField (varName).get (obj);
                    bodyHtml.replace (forBjsTmp.get (0).startIndex , forBjsTmp.get (0).endIndex , invoke.toString ().trim ());
                    forBjsTmp = new ArrayList <> (getBjs (bodyHtml.toString ()));
                }
            }
            else
            {
                // agar az on object nabashe baraye barasi be in method bere
                final String invoke = getValueClass (bj);
                bodyHtml.replace (forBjsTmp.get (0).startIndex , forBjsTmp.get (0).endIndex , invoke.trim ());
                forBjsTmp = new ArrayList <> (getBjs (bodyHtml.toString ()));
            }
        }
        return bodyHtml.toString ();
    }

    private boolean isMethod (final String value , final String var)
    {
        if (var != null) return value.matches (var + "\\.(\\s*(\r|\n|\r\n)*)" + REGEX_METHOD);
        else return value.matches (REGEX_METHOD);
    }

    private String getValueClass (final Bjs bjs) throws Exception
    {
        if (bjs.value == null) return "null";

        // ba dot joda mikonam ke object ba method name ro joda konam (obj.method())
        final String[] split = bjs.value.split ("\\.");

        // bayad dota bashe chone faghat object va method bayad ersal beshe
        if (split.length == 2)
        {
            // bayad dota bashe chone faghat object va method bayad ersal beshe
            final String objName = split[0].trim ();
            final Param param = getParam (objName);

            // agar in method bashe ba object , yani obj.method() => be in sorat bashe
            if (bjs.value.matches ("([a-zA-Z0-9]*|(\\s*|(\r|\n|\r\n)*))\\.(\\s*|(\r|\n|\r\n)*)" + REGEX_METHOD))
            {
                // ba dot joda mikonam ke object ba method name ro joda konam (obj.method())
                final String methodNameWithCircleBracket = split[1].trim ();
                if (!Str.isEmpty (objName) && !Str.isEmpty (methodNameWithCircleBracket))
                {
                    // az 0 ta two mishe ghabl az parantez aval ();
                    final String methodName = methodNameWithCircleBracket.trim ().split ("\\(\\)")[0].trim ();
                    if (param != null)
                    {
                        final Object aClass = param.aClass;
                        if (aClass != null)
                        {
                            final Object val = aClass.getClass ().getMethod (methodName).invoke (aClass);
                            return ((val == null) ? "null" : val.toString ());
                        }
                        else throw new Exception ("Class is null {" + param.varName + "}");
                    }
                    else
                    {
                        // inja bayad class import shode bashad va method ya var static mashad
                        final String packageName = getPackageName (objName);
                        try
                        {
                            final Class <?> aClass = Class.forName (packageName);
                            final Object val = aClass.getMethod (methodName).invoke (aClass);
                            return ((val == null) ? "null" : val.toString ());
                        }
                        catch (NullPointerException e)
                        {
                            throw new ClassNotFoundException (objName);
                        }
                    }
                }
                else throw new Exception ("invalid object.method() " + bjs.value + "}");
            }
            // agar in var bashe ba object , yani obj.var => be in sorat bashe
            else if (bjs.value.matches ("([a-zA-Z0-9]*|(\\s*|(\r|\n|\r\n)*))\\.(\\s*|(\r|\n|\r\n)*)" + REGEX_VAR))
            {
                // az 0 ta two mishe ghabl az parantez aval ();
                final String varName = split[1].split (";")[0].trim ();
                if (!Str.isEmpty (varName))
                {
                    if (param != null)
                    {
                        final Object aClass = param.aClass;
                        if (aClass != null)
                        {
                            final Object val = aClass.getClass ().getField (varName).get (aClass);
                            return ((val == null) ? "null" : val.toString ());
                        }
                        else throw new Exception ("Class is null {" + param.varName + "}");
                    }
                    else
                    {
                        // inja bayad class import shode bashad va method ya var static mashad
                        final String packageName = getPackageName (objName);
                        try
                        {
                            final Class <?> aClass = Class.forName (packageName);
                            final Object val = aClass.getField (varName).get (aClass);
                            return ((val == null) ? "null" : val.toString ());
                        }
                        catch (NullPointerException e)
                        {
                            throw new ClassNotFoundException (objName);
                        }
                    }
                }
                else throw new Exception ("invalid var " + bjs.value + "}");
            }
            // bayad hatman object ersal beshe method ya var khaly ghabol nemishe
            else throw new Exception ("Not found object {" + bjs.value + "}");
        }
        else
        {
            final String varName = bjs.value.split (";")[0].trim ();

            final Param param = getParam (varName);

            if (param != null)
            {
                final Object obj = param.aClass;
                return obj.getClass ().getMethod ("toString").invoke (obj).toString ();
            }
            else
            {
                final Class <?> aClass = ((Object) varName).getClass ();
                return aClass.getMethod ("toString").invoke (varName).toString ();
            }
        }
    }

    // class import shode ra migirad ba package
    private String getPackageName (final String className)
    {
        for (Bjs anImport : imports)
        {
            final String[] importSplit = anImport.value.split ("\\.");
            if (importSplit.length > 0)
            {
                final String importClassName = importSplit[importSplit.length - 1].trim ();
                if (!Str.isEmpty (importClassName))
                {
                    if (className.equals (importClassName.substring (0 , importClassName.length () - 1)))
                    {
                        final String[] splitAnImport = anImport.value.split ("import(\\s|(\r|\n|\r\n)*)");
                        if (splitAnImport.length == 2)
                        {
                            final String packageName = splitAnImport[1].trim ();
                            if (!Str.isEmpty (packageName)) return packageName.split (";")[0];
                        }
                    }
                }
            }
        }
        return null;
    }

    // openClose => open: {  , close: }
    private int getBracket (final int index , final int toIndex , final String openClose) throws Exception
    {
        int counter = 0;
        if (index < toIndex)
        {
            for (int i = index; i < toIndex; i++)
            {
                counter++;
                if (bjs.get (i).value.equals (openClose))
                {
                    try
                    {
                        getBracket (index + 1 , i , "{");
                        break;
                    }
                    catch (Exception e)
                    {
                        return counter;
                    }
                }
            }
        }
        throw new Exception ("Not found close for body: }");
    }

    private Param getParam (final String varName)
    {
        for (Param param : params) if (varName.equals (param.varName)) return param;
        return null;
    }

    private static final class Bjs
    {
        private final int startIndex, endIndex;
        private final String value;

        public Bjs (final int startIndex , final int endIndex , final String value)
        {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.value = value;
        }
    }

    public static final class Param
    {
        // classi ke ersal mikone
        public final Object aClass;

        // nami ke dakhele html hast
        public final String varName;

        private Param (final Object aClass , final String varName)
        {
            this.aClass = aClass;
            this.varName = varName;
        }

        public static Param put (final Object aClass , final String varName)
        {
            return (new Param (aClass , varName));
        }
    }

    public String getHtml ()
    {
        return html;
    }
}
