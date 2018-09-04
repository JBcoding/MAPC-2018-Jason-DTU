append([],L,L). 
append([H|T],L2,[H|L3]) :- append(T,L2,L3).

delete(A, [A|B], B).
delete(A, [B, C|D], [B|E]) :- delete(A, [C|D], E).
