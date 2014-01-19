package com.sun.codemodel;

import java.util.LinkedList;

/**
 * This class works around a bug in CodeModel where _elseif generates
 * <pre>{@code
 *     if (test1) {
 *        ...
 *     } else {
 *        if (test2) { ... }
 *     }
 * }</pre>
 * instead of
 * <pre>{@code
 *     if (test1) {
 *        ...
 *     } else if (test2) {
 *        ...
 *     }
 * }</pre>
 *
 * Usage:
 * <pre>{@code
 * JConditionalFix i = JConditionalFix._if( JExpr.ref("if_condition") );
 * method.body().add(i);
 * i._then().add( JExpr.invoke("if_body") );
 * i._elseif(JExpr.ref("else_if_condition_1"))._then().add( JExpr.invoke("else_if_body_1") );
 * i._elseif(JExpr.ref("else_if_condition_2"))._then().add( JExpr.invoke("else_if_body_2") );
 * i._else().add( JExpr.invoke("else_body") );
 * }</pre>
 *
 * @author p.halicz
 */

public class JConditionalFix implements JStatement {
    public JConditionalFix _if = null;
    private LinkedList<JConditionalFix> _elseList = new LinkedList<JConditionalFix>();

    /**
     * JExpression to test - can be null for else statement.
     */
    private JExpression test = null;

    /**
     * JBlock of statement.
     */
    private JBlock _then = new JBlock();

    /**
     * Constructor
     *
     * @param test JExpression
     */
    JConditionalFix(JConditionalFix _if, JExpression test) {
        this._if = _if;
        this.test = test;
    }

    /**
     * Creates new if/elseif/.../else statement.
     *
     * @param test JExpression
     * @return
     */
    public static JConditionalFix _if(JExpression test) {
        return new JConditionalFix(null, test);
    }

    /**
     * Return the statement block
     *
     * @return Then block
     */
    public JBlock _then() {
        return _then;
    }

    private JConditionalFix getIf() {
        if (this._if != null) {
            return this._if;
        } else {
            return this;
        }
    }

    /**
     * Throws runtime exception when closing else statement already exists.
     */
    private void checkAdd() {
        if (getIf()._elseList.size() == 0) {
            return;
        }

        JConditionalFix lastElse = getIf()._elseList.getLast();

        if (lastElse._if == null) {
            throw new RuntimeException("else statement already exists");
        }
    }

    /**
     * Create a block to be executed by "else" branch
     *
     * @return Newly generated else block
     */
    public JBlock _else() {
        checkAdd();
        JConditionalFix el = new JConditionalFix(this, null);
        getIf()._elseList.addLast(el);
        return el._then();

    }

    /**
     * Creates <tt>... else if(...) ...</tt> code.
     */

    public JConditionalFix _elseif(JExpression test) {
        checkAdd();
        JConditionalFix el = new JConditionalFix(this, test);
        getIf()._elseList.addLast(el);
        return el;
    }

    private boolean isIf() {
        return _if == null;
    }

    private boolean isElseIf() {
        return _if != null && test != null;
    }

    @Override
    public void state(JFormatter f) {
        if (isIf()) {
            stateIf(f);

            for (JConditionalFix el : _elseList) {
                el.state(f);
            }

            f.nl();
        } else if (isElseIf()) {
            stateElseIf(f);
        } else {
            stateElse(f);
        }
    }

    private void stateElse(JFormatter f) {
        f.p(" else ").g(_then);
    }

    private void stateElseIf(JFormatter f) {
        if (JOp.hasTopOp(test)) {
            f.p(" else if ").g(test);
        } else {
            f.p(" else if (").g(test).p(')');
        }

        f.g(_then);
    }

    private void stateIf(JFormatter f) {
        if (JOp.hasTopOp(test)) {
            f.p("if ").g(test);
        } else {
            f.p("if (").g(test).p(')');
        }

        f.g(_then);
    }
}
