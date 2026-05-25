import { Modal } from "../modal/Modal";
import { LoginComponent } from "./LoginComponent";

export function LoginModal({ open, onClose }: { open: boolean, onClose: () => void }) {
  return (
    <Modal open={open} onClose={onClose}>
      <LoginComponent />
    </Modal>
  );
}