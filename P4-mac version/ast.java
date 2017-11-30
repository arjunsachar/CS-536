import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a Moo program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and ids contain line and character number
// information; for string literals and identifiers, they also contain a
// string; for integer literals, they also contain an integer value.
//
// Here are all the different kinds of AST nodes and what kinds of children
// they have.  All of these kinds of AST nodes are subclasses of "ASTnode".
// Indentation indicates further subclassing:
//
//     Subclass            Kids
//     --------            ----
//     ProgramNode         DeclListNode
//     DeclListNode        linked list of DeclNode
//     DeclNode:
//       VarDeclNode       TypeNode, IdNode, int
//       FnDeclNode        TypeNode, IdNode, FormalsListNode, FnBodyNode
//       FormalDeclNode    TypeNode, IdNode
//       StructDeclNode    IdNode, DeclListNode
//
//     FormalsListNode     linked list of FormalDeclNode
//     FnBodyNode          DeclListNode, StmtListNode
//     StmtListNode        linked list of StmtNode
//     ExpListNode         linked list of ExpNode
//
//     TypeNode:
//       IntNode           -- none --
//       BoolNode          -- none --
//       VoidNode          -- none --
//       StructNode        IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       IntLitNode          -- none --
//       StrLitNode          -- none --
//       TrueNode            -- none --
//       FalseNode           -- none --
//       IdNode              -- none --
//       DotAccessNode       ExpNode, IdNode
//       AssignNode          ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         AndNode
//         OrNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         GreaterNode
//         LessEqNode
//         GreaterEqNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of kids, or
// internal nodes with a fixed number of kids:
//
// (1) Leaf nodes:
//        IntNode,   BoolNode,  VoidNode,  IntLitNode,  StrLitNode,
//        TrueNode,  FalseNode, IdNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, FormalsListNode, StmtListNode, ExpListNode
//
// (3) Internal nodes with fixed numbers of kids:
//        ProgramNode,     VarDeclNode,     FnDeclNode,     FormalDeclNode,
//        StructDeclNode,  FnBodyNode,      StructNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, ReadStmtNode,   WriteStmtNode   
//        IfStmtNode,      IfElseStmtNode,  WhileStmtNode,  CallStmtNode
//        ReturnStmtNode,  DotAccessNode,   AssignExpNode,  CallExpNode,
//        UnaryExpNode,    BinaryExpNode,   UnaryMinusNode, NotNode,
//        PlusNode,        MinusNode,       TimesNode,      DivideNode,
//        AndNode,         OrNode,          EqualsNode,     NotEqualsNode,
//        LessNode,        GreaterNode,     LessEqNode,     GreaterEqNode
//
// **********************************************************************

// **********************************************************************
// ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);
    abstract public void nameAnalysis(SymTable symTab);

    // this method can be used by the unparse methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
        for (int k=0; k<indent; k++) p.print(" ");
    }

    

}

// **********************************************************************
// ProgramNode,  DeclListNode, FormalsListNode, FnBodyNode,
// StmtListNode, ExpListNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    /**
     * Sample name analysis method. 
     * Creates an empty symbol table for the outermost scope, then processes
     * all of the globals, struct defintions, and functions in the program.
     */
    public void nameAnalysis(SymTable symTab) {
	    myDeclList.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    // 1 kid
    private DeclListNode myDeclList;
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    // the name analyzor that be used by the outer layer's decl list
    public void nameAnalysis(SymTable symTab) {
	    Iterator it = myDecls.iterator();
	    try {
	        while (it.hasNext()) {
		       ((DeclNode)it.next()).nameAnalysis(symTab);
	        }
	    } catch (Exception ex) {
            //throw ex;
	        System.err.println("unexpected Exception " + ex + " in DeclListNode.nameAnalysis");
	        System.exit(-1);
	    }  
    }

    // the name analyzor that be used by the struct decl scope's decl list
    public void nameAnalysis(SymTable structDeclSymTab, SymTable symTab){
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()){
                DeclNode currDecl = ((DeclNode)it.next()); // if it can be correctly parsed, this must be a varDecl
                if (currDecl instanceof VarDeclNode) {
                    ((VarDeclNode)currDecl).nameAnalysis(structDeclSymTab, symTab);
                } else {
                    // should not reach here
                }
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }


    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    // list of kids (DeclNodes)
    private List<DeclNode> myDecls;
}

class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    // a method that extract the types of parameters as a list
    public List<String> paramTypesList() {
	    List<String> ptList = new LinkedList<String>();
	    for (FormalDeclNode fdNode:myFormals) {
	        ptList.add((String)fdNode.paramType());
	    }
	    return ptList;
    }

    // a method that extract the names of parameters as a list
    public List<String> paramNamesList() {
	    List<String> pnList = new LinkedList<String>();
	    for (FormalDeclNode fdNode:myFormals) {
	        pnList.add((String)fdNode.paramName());
	    }
	    return pnList;
    }

    public void nameAnalysis(SymTable symTab) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        try {
            while (it.hasNext()) {
                ((FormalDeclNode)it.next()).nameAnalysis(symTab);
            }
        } catch (NoSuchElementException ex) {
            System.out.println("Unexpected NoSuchElementException in FormalsListNode.nameAnalysis");
            System.exit(-1);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    // list of kids (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
}

class FnBodyNode extends ASTnode {
    public FnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    public void nameAnalysis(SymTable symTab) {
	    myDeclList.nameAnalysis(symTab);
	    myStmtList.nameAnalysis(symTab);
    }

    // 2 kids
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void nameAnalysis(SymTable symTab) {
        for (StmtNode stmt:myStmts) {
            ((StmtNode)stmt).nameAnalysis(symTab);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        }
    }

    // list of kids (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public void nameAnalysis(SymTable symTab) {
        for (ExpNode exp:myExps) {
            ((ExpNode)exp).nameAnalysis(symTab);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    // list of kids (ExpNodes)
    private List<ExpNode> myExps;
}

// **********************************************************************
// DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    public void nameAnalysis(SymTable symTab) {}
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    public void nameAnalysis(SymTable symTab) {
	    int line = myId.idLine();
	    int nchar = myId.idChar();
	    if (this.mySize == NOT_STRUCT) { // not a struct type
	        if (myType.typeName().equals("int") || myType.typeName().equals("bool")) {
		        try {
                    SemSym varSym = new SemSym(myType.typeName());
                    String varName = myId.idName();
                    symTab.addDecl(varName, varSym);
                } catch (DuplicateSymException ex) {
                    ErrMsg.fatal(line, nchar, "Multiply declared identifier");
                    return;
                } catch (EmptySymTableException ex) {
                    // catch it
                } catch (Exception ex) {
                    System.out.println(line +":"+ nchar+"Unexpected exception "+ex);
                }
	        } else { // bad declaration
                ErrMsg.fatal(line, nchar, "Non-function declared void");
                // but also need to check if multiply declared
                SemSym checkLocal = symTab.lookupLocal((String)myId.idName());
                if (checkLocal != null) {
                    ErrMsg.fatal(line, nchar, "Multiply declared identifier");
                }		        
	        } 
	    } else { // struct type
            // check if struct type already exist
            HashMap outMost = symTab.getOutMostMap();
            if ((outMost.containsKey(myType.typeName())) && (outMost.get(myType.typeName()) instanceof StructVarSemSym)) {
                try {
                    StructDeclSemSym sds = (StructDeclSemSym)(outMost.get(myType.typeName()));
                    StructVarSemSym svs = new StructVarSemSym(myId.idName(), sds);
                    symTab.addDecl(myId.idName(), svs);
                } catch (DuplicateSymException ex) {
                    ErrMsg.fatal(line, nchar, "Multiply declared identifier");
                } catch (EmptySymTableException ex) {
                    // catch it
                } catch (Exception ex) {
                    System.out.println(line +":"+ nchar+"Unexpected exception "+ex);
                }
            } else {
                IdNode id = ((StructNode)myType).getId();
                ErrMsg.fatal(id.idLine(), id.idChar(), "Invalid name of struct type");
                if (symTab.lookupLocal(myId.idName()) != null) {
                    ErrMsg.fatal(line, nchar, "Multiply declared identifier");
                }
            }
	    }
    }

    // check the variable declared in struct body (the fields of struct)
    public void nameAnalysis(SymTable structDeclSymTab, SymTable symTab) {
        int line = myId.idLine();
        int nchar = myId.idChar();
        SemSym sym = null;
        if (myType.typeName().equals("void")) {
            ErrMsg.fatal(myId.idLine(), myId.idChar(), "Non-function declared void");
            // but also need to check if multiply declared
            SemSym checkLocal = structDeclSymTab.lookupLocal((String)myId.idName());
            if (checkLocal != null) {
                ErrMsg.fatal(myId.idLine(), myId.idChar(), "Multiply declared identifier");
                return;
            }
        } else if (myType.typeName().equals("int") || myType.typeName().equals("bool")) {
            try {
                    SemSym varSym = new SemSym(myType.typeName());
                    String varName = myId.idName();
                    structDeclSymTab.addDecl(varName, varSym);
                } catch (DuplicateSymException ex) {
                    ErrMsg.fatal(line, nchar, "Multiply declared identifier");
                    return;
                } catch (EmptySymTableException ex) {
                    // catch it
                } catch (Exception ex) {
                    System.out.println(line +":"+ nchar+"Unexpected exception "+ex);
                }
        } else { // struct type
            HashMap<String, SemSym> outMost = symTab.getOutMostMap();
            if ((outMost.containsKey(myType.typeName())) && (outMost.get(myType.typeName()) instanceof StructVarSemSym)) {
                try {
                    StructDeclSemSym sds = ((StructVarSemSym)(outMost.get(myType.typeName()))).getStructDeclVar();
                    StructVarSemSym svs = new StructVarSemSym(myId.idName(), sds);
                    structDeclSymTab.addDecl(myId.idName(), svs);
                } catch (DuplicateSymException ex) {
                    ErrMsg.fatal(line, nchar, "Multiply declared identifier");
                    return;
                } catch (EmptySymTableException ex) {
                    // catch it
                } catch (Exception ex) {
                    System.out.println(line +":"+ nchar+"Unexpected exception "+ex);
                }
            } else {
                IdNode id = ((StructNode)myType).getId();
                ErrMsg.fatal(id.idLine(), id.idChar(), "Invalid name of struct type");

            }
        }
        
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(";");    
    }

    // 3 kids
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  // use value NOT_STRUCT if this is not a struct type

    public static int NOT_STRUCT = -1;
}

class FnDeclNode extends DeclNode {
    public FnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    public void nameAnalysis(SymTable symTab){
	    // check if multiply declared function name
	    List<String> ptList = myFormalsList.paramTypesList();
	    List<String> pnList = myFormalsList.paramNamesList();
	    String funName = myId.idName();
	    if (symTab.lookupLocal(funName) == null) {
	        
	        String retType = myType.typeName();
	        FunSemSym fss = new FunSemSym(ptList, retType);
	    }
	    symTab.addScope(); // add a new hashtable to the front of the symTable list
        myFormalsList.nameAnalysis(symTab);
        myBody.nameAnalysis(symTab);
	    
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.out.println("Compiler error: FnDeclNode.nameAnalysis, removeScope");
        } catch (Exception ex) {
            System.out.println("Compiler error: FnDeclNode.nameAnalysis, removeScope");
        }
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("(");
        myFormalsList.unparse(p, 0);
        p.println(") {");
        myBody.unparse(p, indent+4);
        p.println("}\n");
    }

    // 4 kids
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    // return the type of parameter as a string
    public String paramType() {
	    return this.myType.typeName();
    }

    // return the name of parameter as a string
    public String paramName() {
	    return this.myId.idName();
    }

    public void nameAnalysis (SymTable symTab) {
        String type = myType.typeName();
        if (type.equals("void")) {
            ErrMsg.fatal(myId.idLine(), myId.idChar(), "Non-function declared void");
            return;
        } else {
            SemSym sym = new SemSym(type);
            try {
                symTab.addDecl((String)myId.idName(), sym);
                myId.setSym(sym);
            } catch (DuplicateSymException ex) {
                ErrMsg.fatal(myId.idLine(), myId.idChar(), "Multiply declared identifier");
            } catch (EmptySymTableException ex) {
                System.out.println("Unexpected EmptySymTableException in FormalDeclNode.nameAnalysis");
            } catch (Exception ex) {
                System.out.println("Unexpected Exception in FormalDeclNode.nameAnalysis");
            }
        }
    }

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    // 2 kids
    private TypeNode myType;
    private IdNode myId;
}

class StructDeclNode extends DeclNode {
    public StructDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
        myDeclList = declList;
    }

    public void nameAnalysis(SymTable symTab) {
        SymTable structBodyVar = new SymTable();
        myDeclList.nameAnalysis(structBodyVar, symTab);
        StructDeclSemSym structDecl = new StructDeclSemSym((String)myId.idName(), structBodyVar);
        try {
            symTab.addDecl(myId.idName(), structDecl); 
            myId.setSym(new StructDeclSemSym("struct", structBodyVar));
        } catch (DuplicateSymException ex) {
            ErrMsg.fatal(myId.idLine(), myId.idChar(), "Multiply declared identifier");
        } catch (EmptySymTableException ex) {
            System.out.println("Unexpected EmptySymTableException in StructDeclNode.nameAnalysis");
        } catch (Exception ex) {
            System.out.println("Unexpected Exception in StructDeclNode.nameAnalysis");
        }
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("struct ");
	    myId.unparse(p, 0);
	    p.println("{");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("};\n");

    }

    // 2 kids
    private IdNode myId;
    private DeclListNode myDeclList;
}

// **********************************************************************
// TypeNode and its Subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    public void nameAnalysis(SymTable symTab) {}
    abstract String typeName() ;
}

class IntNode extends TypeNode {
    public IntNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("int");
    }

    public String typeName() {
	    return "int";
    }
}

class BoolNode extends TypeNode {
    public BoolNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("bool");
    }

    public String typeName() {
	    return "bool";
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }

    public String typeName() {
	    return "void";
    }
}

class StructNode extends TypeNode {
    public StructNode(IdNode id) {
		myId = id;
    }

    public IdNode getId() {
        return this.myId;
    }

    public String typeName() {
        return myId.idName();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("struct ");
		myId.unparse(p, 0);
    }
	
	// 1 kid
    private IdNode myId;
}

// **********************************************************************
// StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    public void nameAnalysis(SymTable symTab){};
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignNode assign) {
        myAssign = assign;
    }

    public void nameAnalysis(SymTable symTab) {
        myAssign.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(";");
    }

    // 1 kid
    private AssignNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++;");
    }

    // 1 kid
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--;");
    }

    // 1 kid
    private ExpNode myExp;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cin >> ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    // 1 kid (actually can only be an IdNode or an ArrayExpNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("cout << ");
        myExp.unparse(p, 0);
        p.println(";");
    }

    // 1 kid
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.out.println("Unexpected EmptySymTableException in IfStmtNode.nameAnalysis, removeScope");
        } catch (Exception ex) {
            System.out.println("Unexpected Exception in IfStmtNode.nameAnalysis, removeScope");
        }
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // e kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myThenDeclList.nameAnalysis(symTab);
        myThenStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.out.println("Unexpected EmptySymTableException in IfElseStmtNode.nameAnalysis, first removeScope");
        } catch (Exception ex) {
            System.out.println("Unexpected Exception in IfElseStmtNode.nameAnalysis, first removeScope");
        }
        symTab.addScope();
        myElseDeclList.nameAnalysis(symTab);
        myElseStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.out.println("Unexpected EmptySymTableException in IfElseStmtNode.nameAnalysis, second removeScope");
        } catch (Exception ex) {
            System.out.println("Unexpected Exception in IfElseStmtNode.nameAnalysis, second removeScope");
        }       
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if (");
        myExp.unparse(p, 0);
        p.println(") {");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
        doIndent(p, indent);
        p.println("else {");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");        
    }

    // 5 kids
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.out.println("Unexpected EmptySymTableException in WhileStmtNode.nameAnalysis, removeScope");
        } catch (Exception ex) {
            System.out.println("Unexpected Exception in WhileStmtNode.nameAnalysis, removeScope");
        }
    }
	
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while (");
        myExp.unparse(p, 0);
        p.println(") {");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}");
    }

    // 3 kids
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    public void nameAnalysis(SymTable symTab) {
        myCall.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(";");
    }

    // 1 kid
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(";");
    }

    // 1 kid
    private ExpNode myExp; // possibly null
}

// **********************************************************************
// ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    public void nameAnalysis(SymTable symTab){};
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void nameAnalysis(SymTable symTab) {
        return; //no need to check
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StringLitNode extends ExpNode {
    public StringLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void nameAnalysis(SymTable symTab) {
        return; //no need to check
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void nameAnalysis(SymTable symTab) {
        return; // no need to check
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("true");
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void nameAnalysis(SymTable symTab) {
        return; // no need to check
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("false");
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
        this.type = null;
    }

    public void nameAnalysis(SymTable symTab) {
        SemSym sym = symTab.lookupGlobal(myStrVal);
        if (sym == null) {
            ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
        } else {
            this.setSym(sym);
        }
    }

    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (type != null) {
            p.print("(");
            p.print(type.toString());
            p.print(")");
        }
    }

    public String idName() {
	    return myStrVal;
    }

    public int idLine() {
	    return myLineNum;
    }

    public int idChar() {
	    return myCharNum;
    }

    public void setSym(SemSym sym) {
        this.type = sym;
    }

    public SemSym getSym() {
        return this.type;
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private SemSym type;
}

class DotAccessExpNode extends ExpNode {
    public DotAccessExpNode(ExpNode loc, IdNode id) {
        myLoc = loc;	
        myId = id;
        this.type = null;
    }

    public void nameAnalysis(SymTable symTab) {
        SymTable subSymTable = null;
        myLoc.nameAnalysis(symTab);
        // check that the LHS of dotAccess is a struct type that has been declared
        
        if (myLoc instanceof IdNode) {
            SemSym locSym = ((IdNode)myLoc).getSym();
            //System.out.println(locSym.toString());   
            //System.out.println("Reach 1101!");
            if (locSym != null && locSym instanceof StructVarSemSym) {
                StructDeclSemSym structDecl = ((StructVarSemSym)locSym).getStructDeclVar();
                subSymTable = structDecl.getFields();
                //System.out.println("Reach 1104!");
            } else if (locSym == null) {
                // TODO
                return;
            } else { // not a struct type
                ErrMsg.fatal(myId.idLine(), myId.idChar(), "Dot-access of non-struct type");
                return;
            }
        } else if (myLoc instanceof DotAccessExpNode) {// recursive call 
            //System.out.println("Reach 1114!");
            SemSym locSym = ((DotAccessExpNode)myLoc).getSym();
            if (locSym == null) {
                IdNode id = ((DotAccessExpNode)myLoc).getId();
                ErrMsg.fatal(id.idLine(), id.idChar(), "Dot-access of non-struct type");
                return;
            } else if (locSym instanceof StructVarSemSym) {
                StructDeclSemSym structDecl = ((StructVarSemSym)locSym).getStructDeclVar();
                subSymTable = structDecl.getFields();   
            } else {
                ((DotAccessExpNode)myLoc).nameAnalysis(symTab);
            }
        } else {
            //System.out.println("Reach 1127!");
            System.out.println("Compiler Error: DotAccessExpNode");
        } 

        // check the RHS of DotAccess is a field of the appropriate sturct
        if (subSymTable != null) {
            SemSym sym = subSymTable.lookupGlobal(this.myId.idName());
            if (sym == null) { // not a field of this struct
                ErrMsg.fatal(this.myId.idLine(), this.myId.idChar(), "Invalid struct field name");
            } else {
                myId.setSym(sym);
                if (sym instanceof StructVarSemSym) {
                    this.type = (StructVarSemSym)sym;
                }
            }
        } else {
            System.out.println("Compiler error: struct body shouldn't be empty");
        }
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myLoc.unparse(p, 0);
		p.print(").");
		myId.unparse(p, 0);
    }

    public SemSym getSym() {
        return this.type;
    }

    public IdNode getId() {
        return this.myId;
    }

    // 2 kids
    private ExpNode myLoc;	
    private IdNode myId;
    private SemSym type;
}

class AssignNode extends ExpNode {
    public AssignNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }

    public void nameAnalysis(SymTable symTab) {
        myLhs.nameAnalysis(symTab);
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
		if (indent != -1)  p.print("(");
	    myLhs.unparse(p, 0);
		p.print(" = ");
		myExp.unparse(p, 0);
		if (indent != -1)  p.print(")");
    }

    // 2 kids
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }

    public void nameAnalysis(SymTable symTab) {
        myId.nameAnalysis(symTab);
        myExpList.nameAnalysis(symTab);
    }

    // ** unparse **
    public void unparse(PrintWriter p, int indent) {
	    myId.unparse(p, 0);
		p.print("(");
		if (myExpList != null) {
			myExpList.unparse(p, 0);
		}
		p.print(")");
    }

    // 2 kids
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }
    
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }   

    // one child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }

    public void nameAnalysis(SymTable symTab) {
        myExp1.nameAnalysis(symTab);
        myExp2.nameAnalysis(symTab);
    }

    // two kids
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// Subclasses of UnaryExpNode
// **********************************************************************

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(-");
		myExp.unparse(p, 0);
		p.print(")");
    }
}

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(!");
		myExp.unparse(p, 0);
		p.print(")");
    }
}

// **********************************************************************
// Subclasses of BinaryExpNode
// **********************************************************************

class PlusNode extends BinaryExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" + ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class MinusNode extends BinaryExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" - ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class TimesNode extends BinaryExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" * ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class DivideNode extends BinaryExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" / ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class AndNode extends BinaryExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" && ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class OrNode extends BinaryExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" || ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class EqualsNode extends BinaryExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" == ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class NotEqualsNode extends BinaryExpNode {
    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" != ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class LessNode extends BinaryExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" < ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class GreaterNode extends BinaryExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" > ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class LessEqNode extends BinaryExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" <= ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}

class GreaterEqNode extends BinaryExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
	    p.print("(");
		myExp1.unparse(p, 0);
		p.print(" >= ");
		myExp2.unparse(p, 0);
		p.print(")");
    }
}
