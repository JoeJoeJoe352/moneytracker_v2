import { createPortal } from "react-dom";
import './modal.css'

interface ModalProps {
  open: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

export function Modal({ open, onClose, children }: ModalProps) {
  if (!open) return null;

  return createPortal(
    <>
        <div
            className="modal-backdrop fade show"
            onClick={onClose}
        />
        <div className="modal d-block" tabIndex={-1} role="dialog" onClick={onClose}>
            <div className="modal-dialog" role="document">
                <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h5 className="modal-title">Modal title</h5>
                    <button type="button" className="btn-close" onClick={onClose} aria-label="Close">
                    </button>
                </div>
                <div className="modal-body">
                    {children}
                </div>
                <div className="modal-footer">
                    <button type="button" className="btn btn-primary">Save changes</button>
                    <button type="button" className="btn btn-secondary" onClick={onClose}>Close</button>
                </div>
                </div>
            </div>
        </div>
    </>,
    document.body // mindig a body tetejére kerül
  );
}