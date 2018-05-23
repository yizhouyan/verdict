package org.verdictdb.core.sql;

import java.util.List;
import java.util.Optional;

import org.verdictdb.core.logical_query.AbstractColumn;
import org.verdictdb.core.logical_query.AbstractRelation;
import org.verdictdb.core.logical_query.BaseColumn;
import org.verdictdb.core.logical_query.BaseTable;
import org.verdictdb.core.logical_query.ColumnOp;
import org.verdictdb.core.logical_query.ConstantColumn;
import org.verdictdb.core.logical_query.SelectQueryOp;
import org.verdictdb.core.sql.syntax.SyntaxAbstract;
import org.verdictdb.exception.UnexpectedTypeException;
import org.verdictdb.exception.VerdictDbException;

public class RelationToSql {
    
    SyntaxAbstract syntax;
    
    public RelationToSql(SyntaxAbstract syntax) {
        this.syntax = syntax;
    }
    
    public String toSql(AbstractRelation relation) throws VerdictDbException{
        if (relation instanceof BaseTable) {
            throw new UnexpectedTypeException("A base table itself cannot be converted to sql.");
        }
        
        return relationToSqlPart(relation);
    }
    
    String columnToSqlPart(AbstractColumn column) throws UnexpectedTypeException {
        if (column instanceof BaseColumn) {
            BaseColumn base = (BaseColumn) column;
            return quoteName(base.getTableSourceAlias()) + "." + quoteName(base.getColumnName());
        }
        else if (column instanceof ConstantColumn) {
            return ((ConstantColumn) column).getValue().toString();
        }
        else if (column instanceof ColumnOp) {
            ColumnOp columnOp = (ColumnOp) column;
            if (columnOp.getOpType().equals("*")) {
                return "*";
            }
            else if (columnOp.getOpType().equals("avg")) {
                return "avg(" + columnToSqlPart(columnOp.getOperand()) + ")";
            }
            else if (columnOp.getOpType().equals("sum")) {
                return "sum(" + columnToSqlPart(columnOp.getOperand()) + ")";
            }
            else if (columnOp.getOpType().equals("count")) {
                return "count(" + columnToSqlPart(columnOp.getOperand()) + ")";
            }
            else if (columnOp.getOpType().equals("add")) {
                return withParentheses(columnOp.getOperand(0)) + " + " + withParentheses(columnOp.getOperand(1));
            }
            else if (columnOp.getOpType().equals("subtract")) {
                return withParentheses(columnOp.getOperand(0)) + " - " + withParentheses(columnOp.getOperand(1));
            }
            else if (columnOp.getOpType().equals("multiply")) {
                return withParentheses(columnOp.getOperand(0)) + " * " + withParentheses(columnOp.getOperand(1));
            }
            else if (columnOp.getOpType().equals("divide")) {
                return withParentheses(columnOp.getOperand(0)) + " / " + withParentheses(columnOp.getOperand(1));
            }
            else if (columnOp.getOpType().equals("=")) {
                return withParentheses(columnOp.getOperand(0)) + " = " + withParentheses(columnOp.getOperand(1));
            }
            else if (columnOp.getOpType().equals("and")) {
                return withParentheses(columnOp.getOperand(0)) + " and " + withParentheses(columnOp.getOperand(1));
            }
            else if (columnOp.getOpType().equals("or")) {
                return withParentheses(columnOp.getOperand(0)) + " or " + withParentheses(columnOp.getOperand(1));
            }
            else {
                throw new UnexpectedTypeException("Unexpceted opType of column: " + columnOp.getOpType().toString());
            }
        }
        throw new UnexpectedTypeException("Unexpceted argument type: " + column.getClass().toString());
    }
    
    String withParentheses(AbstractColumn column) throws UnexpectedTypeException {
        String sql = columnToSqlPart(column);
        if (column instanceof ColumnOp && ((ColumnOp) column).getOpType().equals("*")) {
            sql = "(" + sql + ")";
        }
        return sql;
    }
    
    String relationToSqlPart(AbstractRelation relation) throws VerdictDbException {
        StringBuilder sql = new StringBuilder();
        
        if (relation instanceof BaseTable) {
            BaseTable base = (BaseTable) relation;
            return quoteName(base.getSchemaName()) + "." + quoteName(base.getTableName());
        }
        
        if (!(relation instanceof SelectQueryOp)) {
            throw new UnexpectedTypeException("Unexpected relation type: " + relation.getClass().toString());
        }
        
        SelectQueryOp sel = (SelectQueryOp) relation;
        // select
        sql.append("select");
        List<AbstractColumn> columns = sel.getSelectList();
        boolean isFirstColumn = true;
        for (AbstractColumn a : columns) {
            if (isFirstColumn) {
                sql.append(" " + columnToSqlPart(a));
                isFirstColumn = false;
            } else {
                sql.append(", " + columnToSqlPart(a));
            }
        }
        
        // from
        sql.append(" from");
        List<AbstractRelation> rels = sel.getFromList();
        boolean isFirstRel = true;
        for (AbstractRelation r : rels) {
            if (isFirstRel) {
                sql.append(" " + relationToSqlPart(r));
                isFirstRel = false;
            } else {
                sql.append(", " + relationToSqlPart(r));
            }
        }
        
        // where
        Optional<AbstractColumn> filter = sel.getFilter();
        if (filter.isPresent()) {
            sql.append(" where ");
            sql.append(columnToSqlPart(filter.get()));
        }
        
        return sql.toString();
    }
    
    String quoteName(String name) {
        String quoteString = syntax.getQuoteString();
        return quoteString + name + quoteString;
    }

}
